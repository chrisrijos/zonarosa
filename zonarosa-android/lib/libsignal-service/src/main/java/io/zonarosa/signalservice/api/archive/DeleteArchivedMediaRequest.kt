/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Delete media from the backup cdn.
 */
class DeleteArchivedMediaRequest(
  @JsonProperty val mediaToDelete: List<ArchivedMediaObject>
) {
  data class ArchivedMediaObject(
    @JsonProperty val cdn: Int,
    @JsonProperty val mediaId: String
  )
}
