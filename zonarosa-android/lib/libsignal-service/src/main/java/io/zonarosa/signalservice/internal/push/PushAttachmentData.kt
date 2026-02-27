/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */
package io.zonarosa.service.internal.push

import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment
import io.zonarosa.service.internal.push.http.CancelationZonaRosa
import io.zonarosa.service.internal.push.http.OutputStreamFactory
import io.zonarosa.service.internal.push.http.ResumableUploadSpec
import java.io.InputStream

/**
 * A bundle of data needed to start an attachment upload.
 */
data class PushAttachmentData(
  val contentType: String?,
  val data: InputStream,
  val dataSize: Long,
  val incremental: Boolean,
  val outputStreamFactory: OutputStreamFactory,
  val listener: ZonaRosaServiceAttachment.ProgressListener?,
  val cancelationZonaRosa: CancelationZonaRosa?,
  val resumableUploadSpec: ResumableUploadSpec
)
