/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgress
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.service.BackupMediaRestoreService

/**
 * Restores any media that was previously optimized and off-loaded into the user's archive. Leverages
 * the same archive restore progress/flow.
 */
class RestoreOptimizedMediaJob private constructor(parameters: Parameters) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(RestoreOptimizedMediaJob::class)
    const val KEY = "RestoreOptimizeMediaJob"

    fun enqueue() {
      val job = RestoreOptimizedMediaJob()
      AppDependencies.jobManager.add(job)
    }

    @JvmStatic
    fun enqueueIfNecessary() {
      if (ZonaRosaStore.backup.backsUpMedia && !ZonaRosaStore.backup.optimizeStorage) {
        AppDependencies.jobManager.add(RestoreOptimizedMediaJob())
      }
    }
  }

  constructor() : this(
    parameters = Parameters.Builder()
      .setQueue("RestoreOptimizeMediaJob")
      .setMaxInstancesForQueue(2)
      .setMaxAttempts(3)
      .build()
  )

  override fun run(): Result {
    if (ZonaRosaStore.backup.optimizeStorage && !ZonaRosaStore.backup.userManuallySkippedMediaRestore) {
      Log.i(TAG, "User is optimizing media and has not skipped restore, skipping.")
      return Result.success()
    }

    if (!ZonaRosaStore.backup.optimizeStorage && ZonaRosaStore.backup.userManuallySkippedMediaRestore) {
      Log.i(TAG, "User is not optimizing media but elected to skip media restore, skipping.")
      return Result.success()
    }

    val restorableAttachments = ZonaRosaDatabase.attachments.getRestorableOptimizedAttachments()

    if (restorableAttachments.isEmpty()) {
      return Result.success()
    }

    val jobManager = AppDependencies.jobManager

    ArchiveRestoreProgress.onStartMediaRestore()

    BackupMediaRestoreService.start(context, context.getString(R.string.BackupStatus__restoring_media))

    restorableAttachments
      .forEach {
        val job = RestoreAttachmentJob.forOffloadedRestore(
          messageId = it.mmsId,
          attachmentId = it.attachmentId,
          queueHash = it.plaintextHash?.contentHashCode() ?: it.remoteKey?.contentHashCode()
        )

        // Intentionally enqueues one at a time for safer attachment transfer state management
        jobManager.add(job)
      }

    ArchiveRestoreProgress.onRestoringMedia()

    RestoreAttachmentJob.Queues.OFFLOAD_RESTORE.forEach { queue ->
      AppDependencies.jobManager.add(CheckRestoreMediaLeftJob(queue))
    }

    return Result.success()
  }

  override fun serialize(): ByteArray? = null
  override fun getFactoryKey(): String = KEY
  override fun onFailure() = Unit

  class Factory : Job.Factory<RestoreOptimizedMediaJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): RestoreOptimizedMediaJob {
      return RestoreOptimizedMediaJob(parameters)
    }
  }
}
