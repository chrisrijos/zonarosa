package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.readToList
import io.zonarosa.core.util.requireLong
import io.zonarosa.core.util.select
import io.zonarosa.messenger.database.ChatFolderTables
import io.zonarosa.messenger.database.ChatFolderTables.ChatFolderTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.storage.StorageSyncHelper

/**
 * Marks all chat folders as needing to be synced for storage service.
 */
internal class SyncChatFoldersMigrationJob(parameters: Parameters = Parameters.Builder().build()) : MigrationJob(parameters) {
  companion object {
    const val KEY = "SyncChatFoldersMigrationJob"

    private val TAG = Log.tag(SyncChatFoldersMigrationJob::class)
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    val folderIds = ZonaRosaDatabase.chatFolders.getAllFoldersForSync()

    ZonaRosaDatabase.chatFolders.markNeedsSync(folderIds)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  override fun shouldRetry(e: Exception): Boolean = false

  private fun ChatFolderTables.getAllFoldersForSync(): List<Long> {
    return readableDatabase
      .select(ChatFolderTable.ID)
      .from(ChatFolderTable.TABLE_NAME)
      .run()
      .readToList { cursor -> cursor.requireLong(ChatFolderTable.ID) }
  }

  class Factory : Job.Factory<SyncChatFoldersMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): SyncChatFoldersMigrationJob {
      return SyncChatFoldersMigrationJob(parameters)
    }
  }
}
