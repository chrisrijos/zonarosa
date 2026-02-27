package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper

/**
 * Migration to copy any existing username to [ZonaRosaStore.account]
 */
internal class CopyUsernameToZonaRosaStoreMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "CopyUsernameToZonaRosaStore"

    val TAG = Log.tag(CopyUsernameToZonaRosaStoreMigrationJob::class.java)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (ZonaRosaStore.account.aci == null || ZonaRosaStore.account.pni == null) {
      Log.i(TAG, "ACI/PNI are unset, skipping.")
      return
    }

    val self = Recipient.self()

    if (self.username.isEmpty || self.username.get().isBlank()) {
      Log.i(TAG, "No username set, skipping.")
      return
    }

    ZonaRosaStore.account.username = self.username.get()

    // New fields in storage service, so we trigger a sync
    ZonaRosaDatabase.recipients.markNeedsSync(self.id)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<CopyUsernameToZonaRosaStoreMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CopyUsernameToZonaRosaStoreMigrationJob {
      return CopyUsernameToZonaRosaStoreMigrationJob(parameters)
    }
  }
}
