/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request to copy and re-encrypt media from the attachments cdn into the backup cdn.
 */
class BatchArchiveMediaRequest(
  @JsonProperty val items: List<ArchiveMediaRequest>
)
