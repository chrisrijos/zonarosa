/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.attachments.toAttachmentPointer
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.MultiDeviceAttachmentBackfillUpdateJobData
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.service.api.crypto.UntrustedIdentityException
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.exceptions.ServerRejectedException
import io.zonarosa.service.internal.push.AddressableMessage
import io.zonarosa.service.internal.push.ConversationIdentifier
import io.zonarosa.service.internal.push.SyncMessage
import io.zonarosa.service.internal.push.SyncMessage.AttachmentBackfillResponse.AttachmentData
import java.io.IOException
import kotlin.time.Duration.Companion.days

/**
 * Tells linked devices about all the attachments that have been re-uploaded for a given [SyncMessage.attachmentBackfillRequest].
 */
class MultiDeviceAttachmentBackfillUpdateJob(
  parameters: Parameters,
  private val targetMessage: AddressableMessage,
  private val targetConversation: ConversationIdentifier,
  private val messageId: Long
) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(MultiDeviceAttachmentBackfillUpdateJob::class)

    const val KEY = "MultiDeviceAttachmentBackfillUpdateJob"

    private val JOB_LIFESPAN = 1.days.inWholeMilliseconds
    private val UPLOAD_THRESHOLD = AttachmentUploadJob.UPLOAD_REUSE_THRESHOLD + JOB_LIFESPAN

    fun enqueue(targetMessage: AddressableMessage, targetConversation: ConversationIdentifier, messageId: Long) {
      AppDependencies.jobManager.add(MultiDeviceAttachmentBackfillUpdateJob(targetMessage, targetConversation, messageId))
    }
  }

  constructor(
    targetMessage: AddressableMessage,
    targetConversation: ConversationIdentifier,
    messageId: Long
  ) : this(
    Parameters.Builder()
      .setLifespan(JOB_LIFESPAN)
      .setMaxAttempts(Parameters.UNLIMITED)
      .addConstraint(NetworkConstraint.KEY)
      .build(),
    targetMessage,
    targetConversation,
    messageId
  )

  override fun getFactoryKey(): String = KEY

  override fun serialize(): ByteArray {
    return MultiDeviceAttachmentBackfillUpdateJobData(
      targetMessage = targetMessage,
      targetConversation = targetConversation,
      messageId = messageId
    ).encode()
  }

  override fun run(): Result {
    val allAttachments = ZonaRosaDatabase.attachments.getAttachmentsForMessage(messageId)
    val syncAttachments: List<DatabaseAttachment> = allAttachments
      .filterNot { it.quote || it.contentType == MediaUtil.LONG_TEXT }
      .sortedBy { it.displayOrder }
    val longTextAttachment: DatabaseAttachment? = allAttachments.firstOrNull { it.contentType == MediaUtil.LONG_TEXT }

    if (syncAttachments.isEmpty() && longTextAttachment == null) {
      Log.w(TAG, "Failed to find any attachments for the message! Sending a missing response.")
      MultiDeviceAttachmentBackfillMissingJob.enqueue(targetMessage, targetConversation)
      return Result.failure()
    }

    val syncMessage = ZonaRosaServiceSyncMessage.forAttachmentBackfillResponse(
      SyncMessage.AttachmentBackfillResponse(
        targetMessage = targetMessage,
        targetConversation = targetConversation,
        attachments = SyncMessage.AttachmentBackfillResponse.AttachmentDataList(
          attachments = syncAttachments.map { it.toAttachmentData() },
          longText = longTextAttachment?.toAttachmentData()
        )
      )
    )

    return try {
      val result = AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(syncMessage)
      if (result.isSuccess) {
        Log.i(TAG, "[${targetMessage.sentTimestamp}] Successfully sent backfill update message response.")
        Result.success()
      } else {
        Log.w(TAG, "[${targetMessage.sentTimestamp}] Non-successful result. Retrying.")
        Result.retry(defaultBackoff())
      }
    } catch (e: ServerRejectedException) {
      Log.w(TAG, e)
      Result.failure()
    } catch (e: IOException) {
      Log.w(TAG, e)
      Result.retry(defaultBackoff())
    } catch (e: UntrustedIdentityException) {
      Log.w(TAG, e)
      Result.failure()
    }
  }

  override fun onFailure() {
    if (isCascadingFailure) {
      Log.w(TAG, "The upload job failed! Enqueuing another instance of the job to notify of the failure.")
      MultiDeviceAttachmentBackfillUpdateJob.enqueue(targetMessage, targetConversation, messageId)
    }
  }

  private fun DatabaseAttachment.toAttachmentData(): AttachmentData {
    return when {
      this.hasData && !this.isInProgress && this.withinUploadThreshold() -> {
        AttachmentData(attachment = this.toAttachmentPointer(context))
      }
      !this.hasData || this.isPermanentlyFailed -> {
        AttachmentData(status = AttachmentData.Status.TERMINAL_ERROR)
      }
      else -> {
        AttachmentData(status = AttachmentData.Status.PENDING)
      }
    }
  }

  private fun DatabaseAttachment.withinUploadThreshold(): Boolean {
    return this.uploadTimestamp > 0 && System.currentTimeMillis() - this.uploadTimestamp < UPLOAD_THRESHOLD
  }

  class Factory : Job.Factory<MultiDeviceAttachmentBackfillUpdateJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): MultiDeviceAttachmentBackfillUpdateJob {
      val data = MultiDeviceAttachmentBackfillUpdateJobData.ADAPTER.decode(serializedData!!)
      return MultiDeviceAttachmentBackfillUpdateJob(
        parameters,
        data.targetMessage!!,
        data.targetConversation!!,
        data.messageId
      )
    }
  }
}
