/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.message

import com.fasterxml.jackson.annotation.JsonProperty

data class SpamTokenMessage(@JsonProperty val token: String?)
