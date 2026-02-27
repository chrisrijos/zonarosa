package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.PreKeysSyncJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Schedules a prekey sync.
 */
internal class PreKeysSyncMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(PreKeysSyncMigrationJob::class.java)
    const val KEY = "PreKeysSyncIndexMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    ZonaRosaStore.misc.lastFullPrekeyRefreshTime = 0
    PreKeysSyncJob.enqueue()
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<PreKeysSyncMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): PreKeysSyncMigrationJob {
      return PreKeysSyncMigrationJob(parameters)
    }
  }
}
