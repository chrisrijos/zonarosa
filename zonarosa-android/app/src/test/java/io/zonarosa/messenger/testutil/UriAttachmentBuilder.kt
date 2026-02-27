package io.zonarosa.messenger.testutil

import android.net.Uri
import io.zonarosa.blurhash.BlurHash
import io.zonarosa.core.models.media.TransformProperties
import io.zonarosa.messenger.attachments.UriAttachment
import io.zonarosa.messenger.audio.AudioHash
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.stickers.StickerLocator

object UriAttachmentBuilder {
  fun build(
    id: Long,
    uri: Uri = Uri.parse("content://$id"),
    contentType: String,
    transferState: Int = AttachmentTable.TRANSFER_PROGRESS_PENDING,
    size: Long = 0L,
    fileName: String = "file$id",
    voiceNote: Boolean = false,
    borderless: Boolean = false,
    videoGif: Boolean = false,
    quote: Boolean = false,
    quoteTargetContentType: String? = null,
    caption: String? = null,
    stickerLocator: StickerLocator? = null,
    blurHash: BlurHash? = null,
    audioHash: AudioHash? = null,
    transformProperties: TransformProperties? = null
  ): UriAttachment {
    return UriAttachment(
      uri,
      contentType,
      transferState,
      size,
      fileName,
      voiceNote,
      borderless,
      videoGif,
      quote,
      quoteTargetContentType,
      caption,
      stickerLocator,
      blurHash,
      audioHash,
      transformProperties
    )
  }
}
