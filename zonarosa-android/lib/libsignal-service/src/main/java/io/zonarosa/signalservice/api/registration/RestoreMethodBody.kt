/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.registration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.zonarosa.service.api.provisioning.RestoreMethod

/**
 * Request and response body used to communicate a quick restore method selection during registration.
 */
data class RestoreMethodBody @JsonCreator constructor(
  @JsonProperty val method: RestoreMethod?
)
