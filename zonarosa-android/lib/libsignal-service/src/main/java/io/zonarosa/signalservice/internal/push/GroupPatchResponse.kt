/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/** Error response when attempting to patch group state. */
data class GroupPatchResponse @JsonCreator constructor(
  @JsonProperty val code: Int?,
  @JsonProperty val message: String?
)
