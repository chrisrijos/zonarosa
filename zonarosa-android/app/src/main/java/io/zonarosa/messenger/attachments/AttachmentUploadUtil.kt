/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.attachments

import android.content.Context
import android.graphics.Bitmap
import io.zonarosa.blurhash.BlurHashEncoder
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.mebiBytes
import io.zonarosa.protos.resumableuploads.ResumableUpload
import io.zonarosa.messenger.mms.PartAuthority
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment.ProgressListener
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentStream
import io.zonarosa.service.internal.push.http.ResumableUploadSpec
import java.io.IOException
import java.util.Objects

/**
 * A place collect common attachment upload operations to allow for code reuse.
 */
object AttachmentUploadUtil {

  private val TAG = Log.tag(AttachmentUploadUtil::class.java)

  /**
   * Foreground notification shows while uploading attachments larger than this.
   */
  val FOREGROUND_LIMIT_BYTES: Long = 10.mebiBytes.inWholeBytes

  /**
   * Builds a [ZonaRosaServiceAttachmentStream] from the provided data, which can then be provided to various upload methods.
   */
  @Throws(IOException::class)
  fun buildZonaRosaServiceAttachmentStream(
    context: Context,
    attachment: Attachment,
    uploadSpec: ResumableUpload,
    cancellationZonaRosa: (() -> Boolean)? = null,
    progressListener: ProgressListener? = null
  ): ZonaRosaServiceAttachmentStream {
    val inputStream = PartAuthority.getAttachmentStream(context, attachment.uri!!)
    val builder = ZonaRosaServiceAttachment.newStreamBuilder()
      .withStream(inputStream)
      .withContentType(attachment.contentType)
      .withLength(attachment.size)
      .withFileName(attachment.fileName)
      .withVoiceNote(attachment.voiceNote)
      .withBorderless(attachment.borderless)
      .withGif(attachment.videoGif)
      .withFaststart(attachment.transformProperties?.mp4FastStart ?: false)
      .withWidth(attachment.width)
      .withHeight(attachment.height)
      .withUploadTimestamp(System.currentTimeMillis())
      .withCaption(attachment.caption)
      .withResumableUploadSpec(ResumableUploadSpec.from(uploadSpec))
      .withCancelationZonaRosa(cancellationZonaRosa)
      .withListener(progressListener)
      .withUuid(attachment.uuid)

    if (MediaUtil.isImageType(attachment.contentType)) {
      builder.withBlurHash(getImageBlurHash(context, attachment))
    } else if (MediaUtil.isVideoType(attachment.contentType)) {
      builder.withBlurHash(getVideoBlurHash(context, attachment))
    }

    return builder.build()
  }

  @Throws(IOException::class)
  private fun getImageBlurHash(context: Context, attachment: Attachment): String? {
    if (attachment.blurHash != null) {
      return attachment.blurHash.hash
    }

    if (attachment.uri == null) {
      return null
    }

    return PartAuthority.getAttachmentStream(context, attachment.uri!!).use { inputStream ->
      BlurHashEncoder.encode(inputStream)
    }
  }

  @Throws(IOException::class)
  private fun getVideoBlurHash(context: Context, attachment: Attachment): String? {
    if (attachment.blurHash != null) {
      return attachment.blurHash.hash
    }

    return MediaUtil.getVideoThumbnail(context, Objects.requireNonNull(attachment.uri), 1000)?.let { bitmap ->
      val thumb = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
      bitmap.recycle()

      Log.i(TAG, "Generated video thumbnail...")
      val hash = BlurHashEncoder.encode(thumb)
      thumb.recycle()

      hash
    }
  }
}
