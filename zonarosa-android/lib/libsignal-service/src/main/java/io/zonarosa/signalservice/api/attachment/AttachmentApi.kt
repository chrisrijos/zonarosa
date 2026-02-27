/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.attachment

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.crypto.AttachmentCipherStreamUtil
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentStream
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.crypto.PaddingInputStream
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.push.AttachmentUploadForm
import io.zonarosa.service.internal.push.PushAttachmentData
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.push.http.AttachmentCipherOutputStreamFactory
import io.zonarosa.service.internal.push.http.ResumableUploadSpec
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

/**
 * Class to interact with various attachment-related endpoints.
 */
class AttachmentApi(
  private val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket,
  private val pushServiceSocket: PushServiceSocket
) {
  /**
   * Gets a v4 attachment upload form, which provides the necessary information to upload an attachment.
   *
   * GET /v4/attachments/form/upload
   * - 200: Success
   * - 413: Too many attempts
   * - 429: Too many attempts
   */
  fun getAttachmentV4UploadForm(): NetworkResult<AttachmentUploadForm> {
    val request = WebSocketRequestMessage.get("/v4/attachments/form/upload")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, AttachmentUploadForm::class)
  }

  /**
   * Gets a resumable upload spec, which can be saved and re-used across upload attempts to resume upload progress.
   */
  fun getResumableUploadSpec(key: ByteArray, iv: ByteArray, uploadForm: AttachmentUploadForm): NetworkResult<ResumableUploadSpec> {
    return getResumableUploadUrl(uploadForm)
      .map { url ->
        ResumableUploadSpec(
          attachmentKey = key,
          attachmentIv = iv,
          cdnKey = uploadForm.key,
          cdnNumber = uploadForm.cdn,
          resumeLocation = url,
          expirationTimestamp = System.currentTimeMillis() + PushServiceSocket.CDN2_RESUMABLE_LINK_LIFETIME_MILLIS,
          headers = uploadForm.headers
        )
      }
  }

  /**
   * Uploads an attachment using the v4 upload scheme.
   */
  fun uploadAttachmentV4(attachmentStream: ZonaRosaServiceAttachmentStream): NetworkResult<AttachmentUploadResult> {
    if (attachmentStream.resumableUploadSpec.isEmpty) {
      throw IllegalStateException("Attachment must have a resumable upload spec!")
    }

    return NetworkResult.fromFetch {
      val resumableUploadSpec = attachmentStream.resumableUploadSpec.get()

      val paddedLength = PaddingInputStream.getPaddedSize(attachmentStream.length)
      val dataStream: InputStream = PaddingInputStream(attachmentStream.inputStream, attachmentStream.length)
      val ciphertextLength = AttachmentCipherStreamUtil.getCiphertextLength(paddedLength)

      val attachmentData = PushAttachmentData(
        contentType = attachmentStream.contentType,
        data = dataStream,
        dataSize = ciphertextLength,
        incremental = attachmentStream.isFaststart,
        outputStreamFactory = AttachmentCipherOutputStreamFactory(resumableUploadSpec.attachmentKey, resumableUploadSpec.attachmentIv),
        listener = attachmentStream.listener,
        cancelationZonaRosa = attachmentStream.cancelationZonaRosa,
        resumableUploadSpec = attachmentStream.resumableUploadSpec.get()
      )

      val digestInfo = pushServiceSocket.uploadAttachment(attachmentData)

      AttachmentUploadResult(
        remoteId = ZonaRosaServiceAttachmentRemoteId.V4(attachmentData.resumableUploadSpec.cdnKey),
        cdnNumber = attachmentData.resumableUploadSpec.cdnNumber,
        key = resumableUploadSpec.attachmentKey,
        digest = digestInfo.digest,
        incrementalDigest = digestInfo.incrementalDigest,
        incrementalDigestChunkSize = digestInfo.incrementalMacChunkSize,
        uploadTimestamp = attachmentStream.uploadTimestamp,
        dataSize = attachmentStream.length,
        blurHash = attachmentStream.blurHash.getOrNull()
      )
    }
  }

  /**
   * Uploads a raw file using the v4 upload scheme. No additional encryption is supplied! Always prefer [uploadAttachmentV4], unless you are using a separate
   * encryption scheme (i.e. like backup files).
   */
  fun uploadPreEncryptedFileToAttachmentV4(uploadForm: AttachmentUploadForm, resumableUploadUrl: String, inputStream: InputStream, inputStreamLength: Long): NetworkResult<Unit> {
    return NetworkResult.fromFetch {
      pushServiceSocket.uploadBackupFile(uploadForm, resumableUploadUrl, inputStream, inputStreamLength)
    }
  }

  fun getResumableUploadUrl(uploadForm: AttachmentUploadForm): NetworkResult<String> {
    return NetworkResult.fromFetch {
      pushServiceSocket.getResumableUploadUrl(uploadForm)
    }
  }
}
