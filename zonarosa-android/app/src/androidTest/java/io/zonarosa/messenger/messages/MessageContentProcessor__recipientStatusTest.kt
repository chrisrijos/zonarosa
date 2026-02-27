package io.zonarosa.messenger.messages

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import okio.ByteString.Companion.toByteString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.messenger.database.GroupReceiptTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.buildWith
import io.zonarosa.messenger.testing.GroupTestingUtils
import io.zonarosa.messenger.testing.GroupTestingUtils.asMember
import io.zonarosa.messenger.testing.MessageContentFuzzer
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.messenger.util.MessageTableTestUtils
import io.zonarosa.service.internal.push.DataMessage
import io.zonarosa.service.internal.push.GroupContextV2

@Suppress("ClassName")
@RunWith(AndroidJUnit4::class)
class MessageContentProcessor__recipientStatusTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  private lateinit var processor: MessageContentProcessor
  private var envelopeTimestamp: Long = 0

  @Before
  fun setup() {
    processor = MessageContentProcessor(harness.context)
    envelopeTimestamp = System.currentTimeMillis()
  }

  /**
   * Process sync group sent text transcript with partial send and then process second sync with recipient update
   * flag set to true with the rest of the send completed.
   */
  @Test
  fun syncGroupSentTextMessageWithRecipientUpdateFollowup() {
    val (groupId, masterKey, groupRecipientId) = GroupTestingUtils.insertGroup(revision = 0, harness.self.asMember(), harness.others[0].asMember(), harness.others[1].asMember())
    val groupContextV2 = GroupContextV2.Builder().revision(0).masterKey(masterKey.serialize().toByteString()).build()

    val initialTextMessage = DataMessage.Builder().buildWith {
      body = MessageContentFuzzer.string()
      groupV2 = groupContextV2
      timestamp = envelopeTimestamp
    }

    processor.process(
      envelope = MessageContentFuzzer.envelope(envelopeTimestamp),
      content = MessageContentFuzzer.syncSentTextMessage(initialTextMessage, deliveredTo = listOf(harness.others[0])),
      metadata = MessageContentFuzzer.envelopeMetadata(harness.self.id, harness.self.id, groupId = groupId),
      serverDeliveredTimestamp = MessageContentFuzzer.fuzzServerDeliveredTimestamp(envelopeTimestamp)
    )

    val threadId = ZonaRosaDatabase.threads.getThreadIdFor(groupRecipientId)!!
    val firstSyncMessages = MessageTableTestUtils.getMessages(threadId)
    val firstMessageId = firstSyncMessages[0].id
    val firstReceiptInfo = ZonaRosaDatabase.groupReceipts.getGroupReceiptInfo(firstMessageId)

    processor.process(
      envelope = MessageContentFuzzer.envelope(envelopeTimestamp),
      content = MessageContentFuzzer.syncSentTextMessage(initialTextMessage, deliveredTo = listOf(harness.others[0], harness.others[1]), recipientUpdate = true),
      metadata = MessageContentFuzzer.envelopeMetadata(harness.self.id, harness.self.id, groupId = groupId),
      serverDeliveredTimestamp = MessageContentFuzzer.fuzzServerDeliveredTimestamp(envelopeTimestamp)
    )

    val secondSyncMessages = MessageTableTestUtils.getMessages(threadId)
    val secondReceiptInfo = ZonaRosaDatabase.groupReceipts.getGroupReceiptInfo(firstMessageId)

    assertThat(firstSyncMessages).hasSize(1)
    assertThat(firstSyncMessages[0].body).isEqualTo(initialTextMessage.body)
    assertThat(firstReceiptInfo.first { it.recipientId == harness.others[0] }.status).isEqualTo(GroupReceiptTable.STATUS_UNDELIVERED)
    assertThat(firstReceiptInfo.first { it.recipientId == harness.others[1] }.status).isEqualTo(GroupReceiptTable.STATUS_UNKNOWN)

    assertThat(secondSyncMessages).hasSize(1)
    assertThat(secondSyncMessages[0].body).isEqualTo(initialTextMessage.body)
    assertThat(secondReceiptInfo.first { it.recipientId == harness.others[0] }.status).isEqualTo(GroupReceiptTable.STATUS_UNDELIVERED)
    assertThat(secondReceiptInfo.first { it.recipientId == harness.others[1] }.status).isEqualTo(GroupReceiptTable.STATUS_UNDELIVERED)
  }
}
