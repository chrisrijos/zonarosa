/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonProperty

data class GcmRegistrationId(
  @JsonProperty val gcmRegistrationId: String,
  @JsonProperty val webSocketChannel: Boolean
)
