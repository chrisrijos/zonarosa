/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Util
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.service.api.attachment.AttachmentUploadResult
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId
import kotlin.random.Random

object AttachmentTableTestUtil {

  fun createUploadResult(attachmentId: AttachmentId, uploadTimestamp: Long = System.currentTimeMillis()): AttachmentUploadResult {
    val databaseAttachment = ZonaRosaDatabase.attachments.getAttachment(attachmentId)!!

    return AttachmentUploadResult(
      remoteId = ZonaRosaServiceAttachmentRemoteId.V4("somewhere-${Random.nextLong()}"),
      cdnNumber = Cdn.CDN_3.cdnNumber,
      key = databaseAttachment.remoteKey?.let { Base64.decode(it) } ?: Util.getSecretBytes(64),
      digest = Random.nextBytes(32),
      incrementalDigest = Random.nextBytes(16),
      incrementalDigestChunkSize = 5,
      uploadTimestamp = uploadTimestamp,
      dataSize = databaseAttachment.size,
      blurHash = databaseAttachment.blurHash?.hash
    )
  }
}
