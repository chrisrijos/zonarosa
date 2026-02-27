/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.messenger.components.settings.app.chats.folders.ChatFolderRecord
import io.zonarosa.messenger.conversationlist.model.ConversationFilter
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.testing.ZonaRosaDatabaseRule
import io.zonarosa.messenger.util.RemoteConfig
import java.util.UUID

@Suppress("ClassName")
class ThreadTableTest_active {

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
  fun givenActiveUnarchivedThread_whenIGetUnarchivedConversationList_thenIExpectThread() {
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, false)

    ZonaRosaDatabase.threads.getUnarchivedConversationList(
      ConversationFilter.OFF,
      false,
      0,
      10,
      allChats
    ).use { threads ->
      assertEquals(1, threads.count)

      val record = ThreadTable.StaticReader(threads, InstrumentationRegistry.getInstrumentation().context).getNext()

      assertNotNull(record)
      assertEquals(record!!.recipient.id, recipient.id)
    }
  }

  @Test
  fun givenInactiveUnarchivedThread_whenIGetUnarchivedConversationList_thenIExpectNoThread() {
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, false)
    ZonaRosaDatabase.threads.deleteConversation(threadId)

    ZonaRosaDatabase.threads.getUnarchivedConversationList(
      ConversationFilter.OFF,
      false,
      0,
      10,
      allChats
    ).use { threads ->
      assertEquals(0, threads.count)
    }

    val threadId2 = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    assertEquals(threadId2, threadId)
  }

  @Test
  fun givenActiveArchivedThread_whenIGetUnarchivedConversationList_thenIExpectNoThread() {
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, false)
    ZonaRosaDatabase.threads.setArchived(setOf(threadId), true)

    ZonaRosaDatabase.threads.getUnarchivedConversationList(
      ConversationFilter.OFF,
      false,
      0,
      10,
      allChats
    ).use { threads ->
      assertEquals(0, threads.count)
    }
  }

  @Test
  fun givenActiveArchivedThread_whenIGetArchivedConversationList_thenIExpectThread() {
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, false)
    ZonaRosaDatabase.threads.setArchived(setOf(threadId), true)

    ZonaRosaDatabase.threads.getArchivedConversationList(
      ConversationFilter.OFF,
      0,
      10
    ).use { threads ->
      assertEquals(1, threads.count)
    }
  }

  @Test
  fun givenInactiveArchivedThread_whenIGetArchivedConversationList_thenIExpectNoThread() {
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, false)
    ZonaRosaDatabase.threads.deleteConversation(threadId)
    ZonaRosaDatabase.threads.setArchived(setOf(threadId), true)

    ZonaRosaDatabase.threads.getArchivedConversationList(
      ConversationFilter.OFF,
      0,
      10
    ).use { threads ->
      assertEquals(0, threads.count)
    }

    val threadId2 = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    assertEquals(threadId2, threadId)
  }

  @Test
  fun givenActiveArchivedThread_whenIDeactivateThread_thenIExpectNoMessages() {
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, false)

    ZonaRosaDatabase.messages.getConversation(threadId).use {
      assertEquals(1, it.count)
    }

    ZonaRosaDatabase.threads.deleteConversation(threadId)

    ZonaRosaDatabase.messages.getConversation(threadId).use {
      assertEquals(0, it.count)
    }
  }
}
