/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.zonarosa.service.api.messages.calls.TurnServerInfo

/**
 * Response body for GetCallingRelays
 */
data class GetCallingRelaysResponse @JsonCreator constructor(
  @JsonProperty("relays") val relays: List<TurnServerInfo>?
)
