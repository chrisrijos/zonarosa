/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * Request body to check if our prekeys match what's on the service.
 */
class CheckRepeatedUsedPreKeysRequest(
  @JsonProperty
  val identityType: String,

  @JsonProperty
  @JsonSerialize(using = ByteArraySerializerBase64NoPadding::class)
  val digest: ByteArray
)
