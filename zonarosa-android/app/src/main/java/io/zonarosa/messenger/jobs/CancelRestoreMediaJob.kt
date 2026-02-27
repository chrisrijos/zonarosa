/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgress
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

class CancelRestoreMediaJob private constructor(parameters: Parameters) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(CancelRestoreMediaJob::class)
    const val KEY = "CancelRestoreMediaJob"

    fun enqueue() {
      AppDependencies.jobManager.add(
        CancelRestoreMediaJob(parameters = Parameters.Builder().build())
      )
    }
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    ZonaRosaStore.backup.userManuallySkippedMediaRestore = true

    ArchiveRestoreProgress.onCancelMediaRestore()

    Log.i(TAG, "Canceling all media restore jobs")
    RestoreAttachmentJob.Queues.ALL.forEach { AppDependencies.jobManager.cancelAllInQueue(it) }

    Log.i(TAG, "Enqueueing check restore media jobs to cleanup")
    RestoreAttachmentJob.Queues.ALL.forEach { AppDependencies.jobManager.add(CheckRestoreMediaLeftJob(it)) }

    return Result.success()
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<CancelRestoreMediaJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CancelRestoreMediaJob {
      return CancelRestoreMediaJob(parameters = parameters)
    }
  }
}
