/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Multi-response data for a batch archive media operation.
 */
class BatchArchiveMediaResponse(
  @JsonProperty val responses: List<BatchArchiveMediaItemResponse>
) {
  class BatchArchiveMediaItemResponse(
    @JsonProperty val status: Int?,
    @JsonProperty val failureReason: String?,
    @JsonProperty val cdn: Int?,
    @JsonProperty val mediaId: String
  )
}
