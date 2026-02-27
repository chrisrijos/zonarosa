package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.ArchiveBackupIdReservationJob

/**
 * Simple migration job to just enqueue a [ArchiveBackupIdReservationJob] to ensure that all users reserve a backupId.
 */
internal class ArchiveBackupIdReservationMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(ArchiveBackupIdReservationMigrationJob::class.java)
    const val KEY = "ArchiveBackupIdReservationMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    AppDependencies.jobManager.add(ArchiveBackupIdReservationJob())
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<ArchiveBackupIdReservationMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): ArchiveBackupIdReservationMigrationJob {
      return ArchiveBackupIdReservationMigrationJob(parameters)
    }
  }
}
