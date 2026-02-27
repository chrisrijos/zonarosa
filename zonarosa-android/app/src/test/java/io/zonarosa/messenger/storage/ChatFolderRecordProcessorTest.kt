package io.zonarosa.messenger.storage

import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.testutil.EmptyLogger
import io.zonarosa.service.api.storage.ZonaRosaChatFolderRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.internal.storage.protos.ChatFolderRecord
import io.zonarosa.service.internal.storage.protos.Recipient
import java.util.UUID

/**
 * Tests for [ChatFolderRecordProcessor]
 */
class ChatFolderRecordProcessorTest {
  companion object {
    val STORAGE_ID: StorageId = StorageId.forChatFolder(byteArrayOf(1, 2, 3, 4))

    @JvmStatic
    @BeforeClass
    fun setUpClass() {
      Log.initialize(EmptyLogger())
    }
  }

  private val testSubject = ChatFolderRecordProcessor()

  @Test
  fun `Given a valid proto with a known name and folder type, assert valid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Folder1"
      position = 1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }

  @Test
  fun `Given an invalid proto with no name, assert invalid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = ""
      position = 1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given a valid proto with no folder type, assert invalid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Folder1"
      position = 1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.UNKNOWN
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given a valid proto with a deleted timestamp and negative position, assert valid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Folder1"
      position = -1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 100L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }

  @Test
  fun `Given an invalid proto with a deleted timestamp and positive position, assert invalid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Folder1"
      position = 1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 100L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given an invalid proto with a negative position, assert invalid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Folder1"
      position = -1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given an invalid proto with a bad id, assert invalid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = "bad".toByteArray().toByteString()
      name = "Folder1"
      position = -1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given an invalid proto with a bad recipient, assert invalid`() {
    // GIVEN
    val proto = ChatFolderRecord.Builder().apply {
      identifier = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Folder1"
      position = 1
      showOnlyUnread = false
      showMutedChats = false
      includeAllIndividualChats = false
      includeAllGroupChats = false
      folderType = ChatFolderRecord.FolderType.CUSTOM
      deletedAtTimestampMs = 0L
      includedRecipients = listOf(Recipient(contact = Recipient.Contact(serviceIdBinary = "bad".toByteArray().toByteString())))
    }.build()
    val record = ZonaRosaChatFolderRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }
}
