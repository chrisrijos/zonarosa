package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.AccountConsistencyWorkerJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient

/**
 * Migration to help cleanup some inconsistent state for ourself in the identity table.
 */
internal class IdentityTableCleanupMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "IdentityTableCleanupMigrationJob"

    val TAG = Log.tag(IdentityTableCleanupMigrationJob::class.java)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (ZonaRosaStore.account.aci == null || ZonaRosaStore.account.pni == null) {
      Log.i(TAG, "ACI/PNI are unset, skipping.")
      return
    }

    if (!ZonaRosaStore.account.hasAciIdentityKey()) {
      Log.i(TAG, "No ACI identity set yet, skipping.")
      return
    }

    if (!ZonaRosaStore.account.hasPniIdentityKey()) {
      Log.i(TAG, "No PNI identity set yet, skipping.")
      return
    }

    AppDependencies.protocolStore.aci().identities().saveIdentityWithoutSideEffects(
      Recipient.self().id,
      ZonaRosaStore.account.aci!!,
      ZonaRosaStore.account.aciIdentityKey.publicKey,
      IdentityTable.VerifiedStatus.VERIFIED,
      true,
      System.currentTimeMillis(),
      true
    )

    AppDependencies.protocolStore.pni().identities().saveIdentityWithoutSideEffects(
      Recipient.self().id,
      ZonaRosaStore.account.pni!!,
      ZonaRosaStore.account.pniIdentityKey.publicKey,
      IdentityTable.VerifiedStatus.VERIFIED,
      true,
      System.currentTimeMillis(),
      true
    )

    AppDependencies.jobManager.add(AccountConsistencyWorkerJob())
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<IdentityTableCleanupMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): IdentityTableCleanupMigrationJob {
      return IdentityTableCleanupMigrationJob(parameters)
    }
  }
}
