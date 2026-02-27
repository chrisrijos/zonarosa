/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.withinTransaction
import io.zonarosa.messenger.R
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgress
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.NotPushRegisteredException
import io.zonarosa.messenger.service.BackupMediaRestoreService
import kotlin.time.Duration.Companion.days

/**
 * Job that is responsible for enqueueing attachment download
 * jobs upon restore.
 */
class BackupRestoreMediaJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  companion object {
    private val TAG = Log.tag(BackupRestoreMediaJob::class.java)

    const val KEY = "BackupRestoreMediaJob"
  }

  constructor() : this(
    Parameters.Builder()
      .addConstraint(NetworkConstraint.KEY)
      .setMaxAttempts(Parameters.UNLIMITED)
      .setMaxInstancesForFactory(2)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onAdded() {
    ArchiveRestoreProgress.onStartMediaRestore()
  }

  override fun onRun() {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.e(TAG, "Not registered, cannot restore!")
      throw NotPushRegisteredException()
    }

    val jobManager = AppDependencies.jobManager
    val batchSize = 500
    val restoreTime = System.currentTimeMillis()

    do {
      val restoreThumbnailJobs: MutableList<RestoreAttachmentThumbnailJob> = mutableListOf()
      val restoreFullAttachmentJobs: MutableList<RestoreAttachmentJob> = mutableListOf()

      val restoreThumbnailOnlyAttachmentsIds: MutableList<AttachmentId> = mutableListOf()
      val notRestorable: MutableList<AttachmentId> = mutableListOf()

      val last30DaysAttachments = ZonaRosaDatabase.attachments.getLast30DaysOfRestorableAttachments(batchSize)
      val remainingSize = batchSize - last30DaysAttachments.size

      val remaining = if (remainingSize > 0) {
        ZonaRosaDatabase.attachments.getOlderRestorableAttachments(batchSize = remainingSize)
      } else {
        listOf()
      }

      val attachmentBatch = last30DaysAttachments + remaining
      val messageIds = attachmentBatch.map { it.mmsId }.toSet()
      val messageMap = ZonaRosaDatabase.messages.getMessages(messageIds).associate { it.id to (it as MmsMessageRecord) }

      for (attachment in attachmentBatch) {
        val isWallpaper = attachment.mmsId == AttachmentTable.WALLPAPER_MESSAGE_ID

        val message = messageMap[attachment.mmsId]
        if (message == null && !isWallpaper) {
          Log.w(TAG, "Unable to find message for ${attachment.attachmentId}, mmsId: ${attachment.mmsId}")
          notRestorable += attachment.attachmentId
          continue
        }

        if (isWallpaper || shouldRestoreFullSize(message!!, restoreTime, ZonaRosaStore.backup.optimizeStorage)) {
          restoreFullAttachmentJobs += RestoreAttachmentJob.forInitialRestore(
            messageId = attachment.mmsId,
            attachmentId = attachment.attachmentId,
            stickerPackId = attachment.stickerPackId,
            queueHash = attachment.plaintextHash?.contentHashCode() ?: attachment.remoteKey?.contentHashCode()
          )
        } else {
          restoreThumbnailJobs += RestoreAttachmentThumbnailJob(
            messageId = attachment.mmsId,
            attachmentId = attachment.attachmentId,
            highPriority = false
          )

          restoreThumbnailOnlyAttachmentsIds += attachment.attachmentId
        }
      }

      ZonaRosaDatabase.rawDatabase.withinTransaction {
        // Mark not restorable thumbnails and attachments as failed
        ZonaRosaDatabase.attachments.setThumbnailRestoreState(notRestorable, AttachmentTable.ThumbnailRestoreState.PERMANENT_FAILURE)
        ZonaRosaDatabase.attachments.setRestoreTransferState(notRestorable, AttachmentTable.TRANSFER_PROGRESS_FAILED)

        // Set thumbnail only attachments as offloaded
        ZonaRosaDatabase.attachments.setRestoreTransferState(restoreThumbnailOnlyAttachmentsIds, AttachmentTable.TRANSFER_RESTORE_OFFLOADED)
      }

      ArchiveRestoreProgress.onProcessStart()

      // Intentionally enqueues one at a time for safer attachment transfer state management
      restoreThumbnailJobs.forEach { jobManager.add(it) }
      restoreFullAttachmentJobs.forEach { jobManager.add(it) }
    } while (restoreThumbnailJobs.isNotEmpty() || restoreFullAttachmentJobs.isNotEmpty() || notRestorable.isNotEmpty())

    BackupMediaRestoreService.start(context, context.getString(R.string.BackupStatus__restoring_media))
    ArchiveRestoreProgress.onRestoringMedia()

    RestoreAttachmentJob.Queues.INITIAL_RESTORE.forEach { queue ->
      jobManager.add(CheckRestoreMediaLeftJob(queue))
    }
  }

  private fun shouldRestoreFullSize(message: MmsMessageRecord, restoreTime: Long, optimizeStorage: Boolean): Boolean {
    return !optimizeStorage || ((restoreTime - message.dateReceived) < 30.days.inWholeMilliseconds)
  }

  override fun onShouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<BackupRestoreMediaJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): BackupRestoreMediaJob {
      return BackupRestoreMediaJob(parameters)
    }
  }
}
