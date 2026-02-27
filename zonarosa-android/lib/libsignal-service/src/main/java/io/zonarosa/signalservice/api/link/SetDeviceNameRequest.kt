/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.link

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for setting the name of a linked device.
 */
data class SetDeviceNameRequest(
  @JsonProperty val deviceName: String
)
