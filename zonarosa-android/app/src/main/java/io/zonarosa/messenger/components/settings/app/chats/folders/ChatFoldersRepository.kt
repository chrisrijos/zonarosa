package io.zonarosa.messenger.components.settings.app.chats.folders

import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper

/**
 * Repository for chat folders that handles creation, deletion, listing, etc.,
 */
object ChatFoldersRepository {

  fun getCurrentFolders(): List<ChatFolderRecord> {
    return ZonaRosaDatabase.chatFolders.getCurrentChatFolders()
  }

  fun getUnreadCountAndEmptyAndMutedStatusForFolders(folders: List<ChatFolderRecord>): HashMap<Long, Triple<Int, Boolean, Boolean>> {
    return ZonaRosaDatabase.chatFolders.getUnreadCountAndEmptyAndMutedStatusForFolders(folders)
  }

  fun createFolder(folder: ChatFolderRecord, includedRecipients: Set<Recipient>, excludedRecipients: Set<Recipient>) {
    val includedChats = includedRecipients.map { recipient -> ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient) }
    val excludedChats = excludedRecipients.map { recipient -> ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient) }
    val updatedFolder = folder.copy(
      includedChats = includedChats,
      excludedChats = excludedChats
    )

    ZonaRosaDatabase.chatFolders.createFolder(updatedFolder)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  fun updateFolder(folder: ChatFolderRecord, includedRecipients: Set<Recipient>, excludedRecipients: Set<Recipient>) {
    val includedChats = includedRecipients.map { recipient -> ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient) }
    val excludedChats = excludedRecipients.map { recipient -> ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient) }
    val updatedFolder = folder.copy(
      includedChats = includedChats,
      excludedChats = excludedChats
    )

    ZonaRosaDatabase.chatFolders.updateFolder(updatedFolder)
    scheduleSync(updatedFolder.id)
  }

  fun deleteFolder(folder: ChatFolderRecord) {
    ZonaRosaDatabase.chatFolders.deleteChatFolder(folder)
    scheduleSync(folder.id)
  }

  fun updatePositions(folders: List<ChatFolderRecord>) {
    ZonaRosaDatabase.chatFolders.updatePositions(folders)
    folders.forEach { scheduleSync(it.id) }
  }

  fun getFolder(id: Long): ChatFolderRecord {
    return ZonaRosaDatabase.chatFolders.getChatFolder(id)!!
  }

  fun getFolderCount(): Int {
    return ZonaRosaDatabase.chatFolders.getFolderCount()
  }

  private fun scheduleSync(id: Long) {
    ZonaRosaDatabase.chatFolders.markNeedsSync(id)
    StorageSyncHelper.scheduleSyncForDataChange()
  }
}
