/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Get response with headers to use to read from archive cdn.
 */
class GetArchiveCdnCredentialsResponse(
  @JsonProperty val headers: Map<String, String>
)
