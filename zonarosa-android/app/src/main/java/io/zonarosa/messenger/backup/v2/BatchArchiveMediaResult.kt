/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2

import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.service.api.archive.BatchArchiveMediaResponse

/**
 * Result of attempting to batch copy multiple attachments at once with helpers for
 * processing the collection of mini-responses.
 */
data class BatchArchiveMediaResult(
  private val response: BatchArchiveMediaResponse,
  private val mediaIdToAttachmentId: Map<String, AttachmentId>,
  private val attachmentIdToMediaName: Map<AttachmentId, String>
) {
  val successfulResponses: Sequence<BatchArchiveMediaResponse.BatchArchiveMediaItemResponse>
    get() = response
      .responses
      .asSequence()
      .filter { it.status == 200 }

  val sourceNotFoundResponses: Sequence<BatchArchiveMediaResponse.BatchArchiveMediaItemResponse>
    get() = response
      .responses
      .asSequence()
      .filter { it.status == 410 }

  fun mediaIdToAttachmentId(mediaId: String): AttachmentId {
    return mediaIdToAttachmentId[mediaId]!!
  }

  fun attachmentIdToMediaName(attachmentId: AttachmentId): String {
    return attachmentIdToMediaName[attachmentId]!!
  }
}
