/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.ArchiveUploadProgress
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import kotlin.time.Duration.Companion.days

/**
 * When run, this will find all of the attachments that need to be uploaded to the archive tier and enqueue [UploadAttachmentToArchiveJob]s for them.
 */
class ArchiveAttachmentBackfillJob private constructor(parameters: Parameters) : Job(parameters) {
  companion object {
    private val TAG = Log.tag(ArchiveAttachmentBackfillJob::class.java)

    const val KEY = "ArchiveAttachmentBackfillJob"
  }

  constructor() : this(
    parameters = Parameters.Builder()
      .setQueue(ArchiveCommitAttachmentDeletesJob.ARCHIVE_ATTACHMENT_QUEUE)
      .setMaxInstancesForQueue(2)
      .setLifespan(30.days.inWholeMilliseconds)
      .setMaxAttempts(Parameters.UNLIMITED)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    if (!ZonaRosaStore.backup.backsUpMedia) {
      Log.w(TAG, "This user doesn't back up media! Skipping. Tier: ${ZonaRosaStore.backup.backupTier}")
      return Result.success()
    }

    val jobs = ZonaRosaDatabase.attachments.getAttachmentsThatNeedArchiveUpload()
      .map { attachmentId -> UploadAttachmentToArchiveJob(attachmentId) }

    ZonaRosaDatabase.attachments.createRemoteKeyForAttachmentsThatNeedArchiveUpload()

    ArchiveUploadProgress.onAttachmentSectionStarted(ZonaRosaDatabase.attachments.getPendingArchiveUploadBytes())

    if (!isCanceled) {
      Log.i(TAG, "Adding ${jobs.size} jobs to backfill attachments.", true)
      AppDependencies.jobManager.addAll(jobs)
    } else {
      Log.w(TAG, "Job was canceled. Not enqueuing backfill.", true)
    }

    return Result.success()
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<ArchiveAttachmentBackfillJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): ArchiveAttachmentBackfillJob {
      return ArchiveAttachmentBackfillJob(parameters)
    }
  }
}
