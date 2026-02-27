package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.MultiDeviceKeysUpdateJob
import io.zonarosa.messenger.jobs.StorageForcePushJob
import io.zonarosa.messenger.jobs.Svr2MirrorJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Migration for when we introduce the Account Entropy Pool (AEP).
 */
internal class AepMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(AepMigrationJob::class.java)
    const val KEY = "AepMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered! Skipping.")
      return
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Not primary, skipping.")
      return
    }

    AppDependencies.jobManager.add(Svr2MirrorJob())
    if (ZonaRosaStore.account.isMultiDevice) {
      AppDependencies.jobManager.add(MultiDeviceKeysUpdateJob())
    }
    AppDependencies.jobManager.add(StorageForcePushJob())
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<AepMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AepMigrationJob {
      return AepMigrationJob(parameters)
    }
  }
}
