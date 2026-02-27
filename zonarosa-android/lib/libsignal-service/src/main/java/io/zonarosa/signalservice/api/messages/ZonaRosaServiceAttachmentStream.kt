/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package io.zonarosa.service.api.messages

import io.zonarosa.service.internal.push.http.CancelationZonaRosa
import io.zonarosa.service.internal.push.http.ResumableUploadSpec
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.Optional
import java.util.UUID

/**
 * Represents a local ZonaRosaServiceAttachment to be sent.
 */
class ZonaRosaServiceAttachmentStream(
  val inputStream: InputStream,
  contentType: String?,
  val length: Long,
  val fileName: Optional<String>,
  val voiceNote: Boolean,
  val isBorderless: Boolean,
  val isGif: Boolean,
  val isFaststart: Boolean,
  val preview: Optional<ByteArray>,
  val width: Int,
  val height: Int,
  val uploadTimestamp: Long,
  val caption: Optional<String>,
  val blurHash: Optional<String>,
  val listener: ProgressListener?,
  val cancelationZonaRosa: CancelationZonaRosa?,
  val resumableUploadSpec: Optional<ResumableUploadSpec>,
  val uuid: UUID?
) : ZonaRosaServiceAttachment(contentType!!), Closeable {
  constructor(
    inputStream: InputStream,
    contentType: String?,
    length: Long,
    fileName: Optional<String>,
    voiceNote: Boolean,
    borderless: Boolean,
    gif: Boolean,
    faststart: Boolean,
    listener: ProgressListener?,
    cancelationZonaRosa: CancelationZonaRosa?
  ) : this(
    inputStream = inputStream,
    contentType = contentType,
    length = length,
    fileName = fileName,
    voiceNote = voiceNote,
    isBorderless = borderless,
    isGif = gif,
    isFaststart = faststart,
    preview = Optional.empty<ByteArray>(),
    width = 0,
    height = 0,
    uploadTimestamp = System.currentTimeMillis(),
    caption = Optional.empty<String>(),
    blurHash = Optional.empty<String>(),
    listener = listener,
    cancelationZonaRosa = cancelationZonaRosa,
    resumableUploadSpec = Optional.empty<ResumableUploadSpec>(),
    uuid = UUID.randomUUID()
  )

  override fun isStream() = true
  override fun isPointer() = false

  @Throws(IOException::class)
  override fun close() {
    inputStream.close()
  }
}
