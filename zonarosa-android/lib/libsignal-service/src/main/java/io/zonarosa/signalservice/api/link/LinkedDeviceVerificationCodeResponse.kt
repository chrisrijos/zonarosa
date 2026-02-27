/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.link

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response object for: GET /v1/devices/provisioning/code
 */
data class LinkedDeviceVerificationCodeResponse(
  @JsonProperty val verificationCode: String,
  @JsonProperty val tokenIdentifier: String
)
