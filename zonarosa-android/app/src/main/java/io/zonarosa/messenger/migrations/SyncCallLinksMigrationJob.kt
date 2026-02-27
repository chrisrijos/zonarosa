/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper

/**
 * Marks all call links as needing to be synced by storage service.
 */
internal class SyncCallLinksMigrationJob @JvmOverloads constructor(parameters: Parameters = Parameters.Builder().build()) : MigrationJob(parameters) {

  companion object {
    const val KEY = "SyncCallLinksMigrationJob"

    private val TAG = Log.tag(SyncCallLinksMigrationJob::class)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (ZonaRosaStore.account.aci == null) {
      Log.w(TAG, "Self not available yet.")
      return
    }

    val callLinkRecipients = ZonaRosaDatabase.callLinks.getAll().map { it.recipientId }.filter {
      try {
        Recipient.resolved(it)
        true
      } catch (e: Exception) {
        Log.e(TAG, "Unable to resolve recipient: $it")
        false
      }
    }

    ZonaRosaDatabase.recipients.markNeedsSync(callLinkRecipients)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<SyncCallLinksMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): SyncCallLinksMigrationJob {
      return SyncCallLinksMigrationJob(parameters)
    }
  }
}
