package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.RemoteConfig

/**
 * There was an old bug that resulted in some users having their backup tier set to FREE.
 * This fixes that.
 */
internal class ResetArchiveTierMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(ResetArchiveTierMigrationJob::class.java)
    const val KEY = "ResetArchiveTierMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (ZonaRosaStore.backup.backupTier == null) {
      Log.i(TAG, "No backup tier set. Skipping.")
      return
    }

    if (RemoteConfig.internalUser) {
      Log.i(TAG, "Internal user. Skipping.")
      return
    }

    Log.w(TAG, "Non-internal user had backup tier set: ${ZonaRosaStore.backup.backupTier}. Resetting.")
    ZonaRosaStore.backup.backupTier = null
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<ResetArchiveTierMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): ResetArchiveTierMigrationJob {
      return ResetArchiveTierMigrationJob(parameters)
    }
  }
}
