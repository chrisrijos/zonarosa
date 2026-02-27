/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.deleteAll
import io.zonarosa.messenger.components.settings.app.chats.folders.ChatFolderId
import io.zonarosa.messenger.components.settings.app.chats.folders.ChatFolderRecord
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.service.api.storage.ZonaRosaChatFolderRecord
import io.zonarosa.service.api.storage.StorageId
import java.util.UUID
import io.zonarosa.service.internal.storage.protos.ChatFolderRecord as RemoteChatFolderRecord
import io.zonarosa.service.internal.storage.protos.Recipient as RemoteRecipient

@RunWith(AndroidJUnit4::class)
class ChatFolderTablesTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  private lateinit var alice: RecipientId
  private lateinit var bob: RecipientId
  private lateinit var charlie: RecipientId

  private lateinit var folder1: ChatFolderRecord
  private lateinit var folder2: ChatFolderRecord
  private lateinit var folder3: ChatFolderRecord
  private lateinit var folder4: ChatFolderRecord

  private lateinit var recipientIds: List<RecipientId>

  private var aliceThread: Long = 0
  private var bobThread: Long = 0
  private var charlieThread: Long = 0

  @Before
  fun setUp() {
    recipientIds = createRecipients(5)

    alice = recipientIds[0]
    bob = recipientIds[1]
    charlie = recipientIds[2]

    aliceThread = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(alice))
    bobThread = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(bob))
    charlieThread = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(charlie))

    folder1 = ChatFolderRecord(
      id = 2,
      name = "folder1",
      position = 0,
      includedChats = listOf(aliceThread, bobThread),
      excludedChats = listOf(charlieThread),
      showUnread = true,
      showMutedChats = true,
      showIndividualChats = true,
      folderType = ChatFolderRecord.FolderType.CUSTOM,
      chatFolderId = ChatFolderId.generate(),
      storageServiceId = StorageId.forChatFolder(byteArrayOf(1, 2, 3))
    )

    folder2 = ChatFolderRecord(
      name = "folder2",
      position = 2,
      includedChats = listOf(bobThread),
      showUnread = true,
      showMutedChats = true,
      showIndividualChats = true,
      folderType = ChatFolderRecord.FolderType.INDIVIDUAL,
      chatFolderId = ChatFolderId.generate(),
      storageServiceId = StorageId.forChatFolder(byteArrayOf(2, 3, 4))
    )

    folder3 = ChatFolderRecord(
      name = "folder3",
      position = 3,
      includedChats = listOf(bobThread),
      excludedChats = listOf(aliceThread, charlieThread),
      showUnread = true,
      showMutedChats = true,
      showGroupChats = true,
      folderType = ChatFolderRecord.FolderType.GROUP,
      chatFolderId = ChatFolderId.generate(),
      storageServiceId = StorageId.forChatFolder(byteArrayOf(3, 4, 5))
    )

    folder4 = ChatFolderRecord(
      name = "folder4",
      position = 4,
      excludedChats = listOf(aliceThread, charlieThread),
      showUnread = true,
      showMutedChats = true,
      showGroupChats = true,
      folderType = ChatFolderRecord.FolderType.UNREAD,
      chatFolderId = ChatFolderId.generate(),
      storageServiceId = StorageId.forChatFolder(byteArrayOf(4, 5, 6))
    )

    ZonaRosaDatabase.chatFolders.writableDatabase.deleteAll(ChatFolderTables.ChatFolderTable.TABLE_NAME)
    ZonaRosaDatabase.chatFolders.writableDatabase.deleteAll(ChatFolderTables.ChatFolderMembershipTable.TABLE_NAME)
  }

  @Test
  fun givenChatFolder_whenIGetFolder_thenIExpectFolderWithChats() {
    ZonaRosaDatabase.chatFolders.createFolder(folder1)
    val actualFolders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()

    assertEquals(listOf(folder1), actualFolders)
  }

  @Test
  fun givenChatFolder_whenIUpdateFolder_thenIExpectUpdatedFolderWithChats() {
    ZonaRosaDatabase.chatFolders.createFolder(folder2)
    val folder = ZonaRosaDatabase.chatFolders.getCurrentChatFolders().first()
    val updatedFolder = folder.copy(
      name = "updatedFolder2",
      position = 1,
      includedChats = listOf(aliceThread, charlieThread),
      excludedChats = listOf(bobThread)
    )
    ZonaRosaDatabase.chatFolders.updateFolder(updatedFolder)

    val actualFolder = ZonaRosaDatabase.chatFolders.getCurrentChatFolders().first()

    assertEquals(updatedFolder, actualFolder)
  }

  @Test
  fun givenADeletedChatFolder_whenIGetFolders_thenIExpectAListWithoutThatFolder() {
    ZonaRosaDatabase.chatFolders.createFolder(folder1)
    ZonaRosaDatabase.chatFolders.createFolder(folder2)
    val folders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()
    ZonaRosaDatabase.chatFolders.deleteChatFolder(folders.last())

    val actualFolders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()

    assertEquals(listOf(folder1), actualFolders)
  }

  @Test
  fun givenChatFolders_whenIUpdateTheirStorageSyncIds_thenIExpectAnUpdatedList() {
    val existingMap = ZonaRosaDatabase.chatFolders.getStorageSyncIdsMap()
    existingMap.forEach { (id, _) ->
      ZonaRosaDatabase.chatFolders.applyStorageIdUpdate(id, StorageId.forChatFolder(StorageSyncHelper.generateKey()))
    }
    val updatedMap = ZonaRosaDatabase.chatFolders.getStorageSyncIdsMap()

    existingMap.forEach { (id, storageId) ->
      assertNotEquals(storageId, updatedMap[id])
    }
  }

  @Test
  fun givenARemoteFolder_whenIInsertLocally_thenIExpectAListWithThatFolder() {
    val remoteRecord =
      ZonaRosaChatFolderRecord(
        folder1.storageServiceId!!,
        RemoteChatFolderRecord(
          identifier = UuidUtil.toByteArray(folder1.chatFolderId.uuid).toByteString(),
          name = folder1.name,
          position = folder1.position,
          showOnlyUnread = folder1.showUnread,
          showMutedChats = folder1.showMutedChats,
          includeAllIndividualChats = folder1.showIndividualChats,
          includeAllGroupChats = folder1.showGroupChats,
          folderType = RemoteChatFolderRecord.FolderType.CUSTOM,
          deletedAtTimestampMs = folder1.deletedTimestampMs,
          includedRecipients = listOf(
            RemoteRecipient(RemoteRecipient.Contact(Recipient.resolved(alice).serviceId.get().toString())),
            RemoteRecipient(RemoteRecipient.Contact(Recipient.resolved(bob).serviceId.get().toString()))
          ),
          excludedRecipients = listOf(
            RemoteRecipient(RemoteRecipient.Contact(Recipient.resolved(charlie).serviceId.get().toString()))
          )

        )
      )

    ZonaRosaDatabase.chatFolders.insertChatFolderFromStorageSync(remoteRecord)
    val actualFolders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()

    assertEquals(listOf(folder1), actualFolders)
  }

  @Test
  fun givenADeletedChatFolder_whenIGetPositions_thenIExpectPositionsToStillBeConsecutive() {
    ZonaRosaDatabase.chatFolders.createFolder(folder1)
    ZonaRosaDatabase.chatFolders.createFolder(folder2)
    ZonaRosaDatabase.chatFolders.createFolder(folder3)

    val folders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()
    ZonaRosaDatabase.chatFolders.deleteChatFolder(folders[1])

    val actualFolders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()
    actualFolders.forEachIndexed { index, folder ->
      assertEquals(folder.position, index)
    }
  }

  @Test
  fun givenAnEmptyFolder_whenIGetItsEmptyStatus_thenIExpectTrue() {
    ZonaRosaDatabase.chatFolders.createFolder(folder4)
    val actualFolders = ZonaRosaDatabase.chatFolders.getCurrentChatFolders()
    val unreadCountAndEmptyAndMutedStatus = ZonaRosaDatabase.chatFolders.getUnreadCountAndEmptyAndMutedStatusForFolders(actualFolders)
    val actualFolderIsEmpty = unreadCountAndEmptyAndMutedStatus[actualFolders.first().id]!!.second

    assertTrue(actualFolderIsEmpty)
  }

  private fun createRecipients(count: Int): List<RecipientId> {
    return (1..count).map {
      ZonaRosaDatabase.recipients.getOrInsertFromServiceId(ACI.from(UUID.randomUUID()))
    }
  }
}
