/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.link

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data from primary on where to find link+sync backup file.
 */
data class TransferArchiveResponse @JsonCreator constructor(
  @JsonProperty val cdn: Int,
  @JsonProperty val key: String
)
