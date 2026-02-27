package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.BackfillDigestsForDataFileJob

/**
 * Finds all attachments that share a data file and schedules a [BackfillDigestsForDataFileJob] for each.
 */
internal class BackfillDigestsForDuplicatesMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    private val TAG = Log.tag(BackfillDigestsForDuplicatesMigrationJob::class.java)
    const val KEY = "BackfillDigestsForDuplicatesMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    val jobs = ZonaRosaDatabase.attachments.getDataFilesWithMultipleValidAttachments()
      .map { BackfillDigestsForDataFileJob(it) }

    AppDependencies.jobManager.addAll(jobs)

    Log.i(TAG, "Enqueued ${jobs.size} backfill digest jobs for duplicate attachments.")
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<BackfillDigestsForDuplicatesMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): BackfillDigestsForDuplicatesMigrationJob {
      return BackfillDigestsForDuplicatesMigrationJob(parameters)
    }
  }
}
