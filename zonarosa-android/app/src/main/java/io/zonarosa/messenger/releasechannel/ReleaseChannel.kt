package io.zonarosa.messenger.releasechannel

import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.messenger.attachments.PointerAttachment
import io.zonarosa.messenger.database.MessageTable
import io.zonarosa.messenger.database.MessageType
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.StoryType
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList
import io.zonarosa.messenger.mms.IncomingMessage
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId
import java.util.Optional
import java.util.UUID

/**
 * One stop shop for inserting Release Channel messages.
 */
object ReleaseChannel {

  fun insertReleaseChannelMessage(
    recipientId: RecipientId,
    body: String,
    threadId: Long,
    media: String? = null,
    mediaWidth: Int = 0,
    mediaHeight: Int = 0,
    mediaType: String = "image/webp",
    mediaAttachmentUuid: UUID? = UUID.randomUUID(),
    messageRanges: BodyRangeList? = null,
    storyType: StoryType = StoryType.NONE
  ): MessageTable.InsertResult? {
    val attachments: Optional<List<ZonaRosaServiceAttachment>> = if (media != null) {
      val attachment = ZonaRosaServiceAttachmentPointer(
        cdnNumber = Cdn.S3.cdnNumber,
        remoteId = ZonaRosaServiceAttachmentRemoteId.S3,
        contentType = mediaType,
        key = null,
        size = Optional.empty(),
        preview = Optional.empty(),
        width = mediaWidth,
        height = mediaHeight,
        digest = Optional.empty(),
        incrementalDigest = Optional.empty(),
        incrementalMacChunkSize = 0,
        fileName = Optional.of(media),
        voiceNote = false,
        isBorderless = false,
        isGif = MediaUtil.isVideo(mediaType),
        caption = Optional.empty(),
        blurHash = Optional.empty(),
        uploadTimestamp = System.currentTimeMillis(),
        uuid = mediaAttachmentUuid
      )

      Optional.of(listOf(attachment))
    } else {
      Optional.empty()
    }

    val message = IncomingMessage(
      type = MessageType.NORMAL,
      from = recipientId,
      sentTimeMillis = System.currentTimeMillis(),
      serverTimeMillis = System.currentTimeMillis(),
      receivedTimeMillis = System.currentTimeMillis(),
      body = body,
      attachments = PointerAttachment.forPointers(attachments),
      serverGuid = UUID.randomUUID().toString(),
      messageRanges = messageRanges,
      storyType = storyType
    )

    return ZonaRosaDatabase.messages.insertMessageInbox(message, threadId).orElse(null)
  }
}
