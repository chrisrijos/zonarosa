/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response body for getting the media items stored in the user's archive.
 */
class ArchiveGetMediaItemsResponse(
  @JsonProperty val storedMediaObjects: List<StoredMediaObject>,
  @JsonProperty val backupDir: String?,
  @JsonProperty val mediaDir: String?,
  @JsonProperty val cursor: String?
) {
  data class StoredMediaObject(
    @JsonProperty val cdn: Int,
    @JsonProperty val mediaId: String,
    @JsonProperty val objectLength: Long
  )
}
