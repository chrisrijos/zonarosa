package io.zonarosa.messenger.database

import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.util.CursorUtil
import io.zonarosa.messenger.components.settings.app.chats.folders.ChatFolderRecord
import io.zonarosa.messenger.conversationlist.model.ConversationFilter
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.testing.ZonaRosaDatabaseRule
import io.zonarosa.messenger.util.RemoteConfig
import java.util.UUID

@Suppress("ClassName")
class ThreadTableTest_pinned {

  @Rule
  @JvmField
  val databaseRule = ZonaRosaDatabaseRule()

  private lateinit var recipient: Recipient
  private val allChats: ChatFolderRecord = ChatFolderRecord(folderType = ChatFolderRecord.FolderType.ALL)

  @Before
  fun setUp() {
    mockkStatic(RemoteConfig::class)

    every { RemoteConfig.showChatFolders } returns true

    recipient = Recipient.resolved(ZonaRosaDatabase.recipients.getOrInsertFromServiceId(ACI.from(UUID.randomUUID())))
  }

  @Test
  fun givenAPinnedThread_whenIDeleteTheLastMessage_thenIDoNotDeleteOrUnpinTheThread() {
    // GIVEN
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId = MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.pinConversations(listOf(threadId))

    // WHEN
    ZonaRosaDatabase.messages.deleteMessage(messageId)

    // THEN
    val pinned = ZonaRosaDatabase.threads.getPinnedThreadIds()
    assertTrue(threadId in pinned)
  }

  @Test
  fun givenAPinnedThread_whenIDeleteTheLastMessage_thenIExpectTheThreadInUnarchivedCount() {
    // GIVEN
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId = MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.pinConversations(listOf(threadId))

    // WHEN
    ZonaRosaDatabase.messages.deleteMessage(messageId)

    // THEN
    val unarchivedCount = ZonaRosaDatabase.threads.getUnarchivedConversationListCount(ConversationFilter.OFF, allChats)
    assertEquals(1, unarchivedCount)
  }

  @Test
  fun givenAPinnedThread_whenIDeleteTheLastMessage_thenIExpectPinnedThreadInUnarchivedList() {
    // GIVEN
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId = MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.pinConversations(listOf(threadId))

    // WHEN
    ZonaRosaDatabase.messages.deleteMessage(messageId)

    // THEN
    ZonaRosaDatabase.threads.getUnarchivedConversationList(ConversationFilter.OFF, true, 0, 1, allChats).use {
      it.moveToFirst()
      assertEquals(threadId, CursorUtil.requireLong(it, ThreadTable.ID))
    }
  }
}
