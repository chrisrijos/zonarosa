package io.zonarosa.messenger.conversation

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.util.ThreadUtil
import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.messenger.attachments.PointerAttachment
import io.zonarosa.messenger.conversation.v2.ConversationActivity
import io.zonarosa.messenger.database.MessageType
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.mms.IncomingMessage
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId
import java.util.Optional

/**
 * Helper test for rendering conversation items for preview.
 */
@RunWith(AndroidJUnit4::class)
@Ignore("For testing/previewing manually, no assertions")
class ConversationItemPreviewer {

  @get:Rule
  val harness = ZonaRosaActivityRule(othersCount = 10)

  @Test
  fun testShowLongName() {
    val other: Recipient = Recipient.resolved(harness.others.first())

    ZonaRosaDatabase.recipients.setProfileName(other.id, ProfileName.fromParts("Seef", "$$$"))

    insertFailedMediaMessage(other = other, attachmentCount = 1)
    insertFailedMediaMessage(other = other, attachmentCount = 2)
    insertFailedMediaMessage(other = other, body = "Test", attachmentCount = 1)
//    insertFailedOutgoingMediaMessage(other = other, body = "Test", attachmentCount = 1)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)
//    insertMediaMessage(other = other)

    val scenario: ActivityScenario<ConversationActivity> = harness.launchActivity { putExtra("recipient_id", other.id.serialize()) }
    scenario.onActivity {
    }

    // Uncomment to make dialog stay on screen, otherwise will show/dismiss immediately
//    ThreadUtil.sleep(45000)
  }

  private fun insertMediaMessage(other: Recipient, body: String? = null, attachmentCount: Int = 1) {
    val attachments: List<ZonaRosaServiceAttachmentPointer> = (0 until attachmentCount).map {
      attachment()
    }

    val message = IncomingMessage(
      type = MessageType.NORMAL,
      from = other.id,
      body = body,
      sentTimeMillis = System.currentTimeMillis(),
      serverTimeMillis = System.currentTimeMillis(),
      receivedTimeMillis = System.currentTimeMillis(),
      attachments = PointerAttachment.forPointers(Optional.of(attachments))
    )

    ZonaRosaDatabase.messages.insertMessageInbox(message, ZonaRosaDatabase.threads.getOrCreateThreadIdFor(other)).get()

    ThreadUtil.sleep(1)
  }

  private fun insertFailedMediaMessage(other: Recipient, body: String? = null, attachmentCount: Int = 1) {
    val attachments: List<ZonaRosaServiceAttachmentPointer> = (0 until attachmentCount).map {
      attachment()
    }

    val message = IncomingMessage(
      type = MessageType.NORMAL,
      from = other.id,
      body = body,
      sentTimeMillis = System.currentTimeMillis(),
      serverTimeMillis = System.currentTimeMillis(),
      receivedTimeMillis = System.currentTimeMillis(),
      attachments = PointerAttachment.forPointers(Optional.of(attachments))
    )

    val insert = ZonaRosaDatabase.messages.insertMessageInbox(message, ZonaRosaDatabase.threads.getOrCreateThreadIdFor(other)).get()

    ZonaRosaDatabase.attachments.getAttachmentsForMessage(insert.messageId).forEachIndexed { index, attachment ->
//      if (index != 1) {
      ZonaRosaDatabase.attachments.setTransferProgressPermanentFailure(attachment.attachmentId, insert.messageId)
//      } else {
//        ZonaRosaDatabase.attachments.setTransferState(insert.messageId, attachment, TRANSFER_PROGRESS_STARTED)
//      }
    }

    ThreadUtil.sleep(1)
  }

  private fun insertFailedOutgoingMediaMessage(other: Recipient, body: String? = null, attachmentCount: Int = 1) {
    val attachments: List<ZonaRosaServiceAttachmentPointer> = (0 until attachmentCount).map {
      attachment()
    }

    val message = OutgoingMessage(
      recipient = other,
      body = body,
      attachments = PointerAttachment.forPointers(Optional.of(attachments)),
      timestamp = System.currentTimeMillis(),
      isSecure = true
    )

    val insert = ZonaRosaDatabase.messages.insertMessageOutbox(
      message,
      ZonaRosaDatabase.threads.getOrCreateThreadIdFor(other),
      false,
      null
    ).messageId

    ZonaRosaDatabase.attachments.getAttachmentsForMessage(insert).forEachIndexed { index, attachment ->
      ZonaRosaDatabase.attachments.setTransferProgressPermanentFailure(attachment.attachmentId, insert)
    }

    ThreadUtil.sleep(1)
  }

  private fun attachment(): ZonaRosaServiceAttachmentPointer {
    return ZonaRosaServiceAttachmentPointer(
      Cdn.CDN_3.cdnNumber,
      ZonaRosaServiceAttachmentRemoteId.from(""),
      "image/webp",
      null,
      Optional.empty(),
      Optional.empty(),
      1024,
      1024,
      Optional.empty(),
      Optional.empty(),
      0,
      Optional.of("/not-there.jpg"),
      false,
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      System.currentTimeMillis(),
      null
    )
  }
}
