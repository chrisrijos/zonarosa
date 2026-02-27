package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient

/**
 * There was a bug where some users had their own recipient entry marked unregistered. This fixes that.
 */
internal class SelfRegisteredStateMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "SelfRegisteredStateMigrationJob"

    val TAG = Log.tag(SelfRegisteredStateMigrationJob::class.java)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (ZonaRosaStore.account.isRegistered && ZonaRosaStore.account.aci != null) {
      val record = ZonaRosaDatabase.recipients.getRecord(Recipient.self().id)

      if (record.registered != RecipientTable.RegisteredState.REGISTERED) {
        Log.w(TAG, "Inconsistent registered state! Fixing...")
        ZonaRosaDatabase.recipients.markRegistered(Recipient.self().id, ZonaRosaStore.account.aci!!)
      } else {
        Log.d(TAG, "Local user is already registered.")
      }
    } else {
      Log.d(TAG, "Not registered. Skipping.")
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<SelfRegisteredStateMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): SelfRegisteredStateMigrationJob {
      return SelfRegisteredStateMigrationJob(parameters)
    }
  }
}
