package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.withinTransaction
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.MultiDeviceStorageSyncRequestJob
import io.zonarosa.messenger.jobs.StorageSyncJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Remove local unknown storage ids not in local storage service manifest.
 */
internal class StorageFixLocalUnknownMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(StorageFixLocalUnknownMigrationJob::class.java)
    const val KEY = "StorageFixLocalUnknownMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  @Suppress("UsePropertyAccessSyntax")
  override fun performMigration() {
    val localStorageIds = ZonaRosaStore.storageService.manifest.storageIds.toSet()
    val unknownLocalIds = ZonaRosaDatabase.unknownStorageIds.getAllUnknownIds().toSet()
    val danglingLocalUnknownIds = unknownLocalIds - localStorageIds

    if (danglingLocalUnknownIds.isEmpty()) {
      return
    }

    Log.w(TAG, "Removing ${danglingLocalUnknownIds.size} dangling unknown ids")

    ZonaRosaDatabase.rawDatabase.withinTransaction {
      ZonaRosaDatabase.unknownStorageIds.delete(danglingLocalUnknownIds)
    }

    val jobManager = AppDependencies.jobManager

    if (ZonaRosaStore.account.isMultiDevice) {
      Log.i(TAG, "Multi-device.")
      jobManager.startChain(StorageSyncJob.forLocalChange())
        .then(MultiDeviceStorageSyncRequestJob())
        .enqueue()
    } else {
      Log.i(TAG, "Single-device.")
      jobManager.add(StorageSyncJob.forRemoteChange())
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<StorageFixLocalUnknownMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): StorageFixLocalUnknownMigrationJob {
      return StorageFixLocalUnknownMigrationJob(parameters)
    }
  }
}
