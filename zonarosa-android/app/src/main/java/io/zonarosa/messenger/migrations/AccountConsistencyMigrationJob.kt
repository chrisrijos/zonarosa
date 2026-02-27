package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.AccountConsistencyWorkerJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Migration to help address some account consistency issues that resulted under very specific situation post-device-transfer.
 */
internal class AccountConsistencyMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "AccountConsistencyMigrationJob"

    val TAG = Log.tag(AccountConsistencyMigrationJob::class.java)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (!ZonaRosaStore.account.hasAciIdentityKey()) {
      Log.i(TAG, "No identity set yet, skipping.")
      return
    }

    if (!ZonaRosaStore.account.isRegistered || ZonaRosaStore.account.aci == null) {
      Log.i(TAG, "Not yet registered, skipping.")
      return
    }

    AppDependencies.jobManager.add(AccountConsistencyWorkerJob())
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<AccountConsistencyMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AccountConsistencyMigrationJob {
      return AccountConsistencyMigrationJob(parameters)
    }
  }
}
