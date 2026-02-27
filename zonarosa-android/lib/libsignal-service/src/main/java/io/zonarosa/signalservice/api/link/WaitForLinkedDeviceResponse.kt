/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.link

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response body for GET /v1/devices/wait_for_linked_device/{tokenIdentifier}
 */
data class WaitForLinkedDeviceResponse(
  @JsonProperty val id: Int,
  @JsonProperty val name: String,
  @JsonProperty val lastSeen: Long,
  @JsonProperty val registrationId: Int,
  @JsonProperty val createdAtCiphertext: String?
)
