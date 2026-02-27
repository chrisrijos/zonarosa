/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

public record AccountCreationResponse(

    @JsonUnwrapped
    AccountIdentityResponse identityResponse,

    @Schema(description = "If true, there was an existing account registered for this number")
    boolean reregistration) {
}
