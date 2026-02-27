package io.zonarosa.messenger.migrations

import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.MultiDeviceKeysUpdateJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Migration to sync keys with linked devices.
 */
internal class SyncKeysMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "SyncKeysMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (ZonaRosaStore.account.isLinkedDevice) {
      return
    }

    if (ZonaRosaStore.account.isMultiDevice) {
      AppDependencies.jobManager.add(MultiDeviceKeysUpdateJob())
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<SyncKeysMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): SyncKeysMigrationJob {
      return SyncKeysMigrationJob(parameters)
    }
  }
}
