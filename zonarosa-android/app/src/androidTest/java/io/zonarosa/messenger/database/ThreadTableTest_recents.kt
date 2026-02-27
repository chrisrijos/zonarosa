package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.util.CursorUtil
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.testing.ZonaRosaDatabaseRule
import java.util.UUID

@Suppress("ClassName")
@RunWith(AndroidJUnit4::class)
class ThreadTableTest_recents {

  @Rule
  @JvmField
  val databaseRule = ZonaRosaDatabaseRule()

  private lateinit var recipient: Recipient

  @Before
  fun setUp() {
    recipient = Recipient.resolved(ZonaRosaDatabase.recipients.getOrInsertFromServiceId(ACI.from(UUID.randomUUID())))
  }

  @Test
  fun givenARecentRecipient_whenIBlockAndGetRecents_thenIDoNotExpectToSeeThatRecipient() {
    // GIVEN
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    MmsHelper.insert(recipient = recipient, threadId = threadId)
    ZonaRosaDatabase.threads.update(threadId, true)

    // WHEN
    ZonaRosaDatabase.recipients.setBlocked(recipient.id, true)
    val results: MutableList<RecipientId> = ZonaRosaDatabase.threads.getRecentConversationList(10, false, false, false, false, false, false).use { cursor ->
      val ids = mutableListOf<RecipientId>()
      while (cursor.moveToNext()) {
        ids.add(RecipientId.from(CursorUtil.requireLong(cursor, ThreadTable.RECIPIENT_ID)))
      }

      ids
    }

    // THEN
    assertFalse(recipient.id in results)
  }
}
