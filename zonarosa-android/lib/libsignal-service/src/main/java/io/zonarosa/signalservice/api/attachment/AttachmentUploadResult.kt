/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.attachment

import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId

/**
 * The result of uploading an attachment. Just the additional metadata related to the upload itself.
 */
class AttachmentUploadResult(
  val remoteId: ZonaRosaServiceAttachmentRemoteId,
  val cdnNumber: Int,
  val key: ByteArray,
  val digest: ByteArray,
  val incrementalDigest: ByteArray?,
  val incrementalDigestChunkSize: Int,
  val dataSize: Long,
  val uploadTimestamp: Long,
  val blurHash: String?
)
