/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record ProvisioningMessage(
    @Schema(description = "The MIME base64-encoded body of the provisioning message to send to the destination device")
    @NotEmpty
    String body) {
}
