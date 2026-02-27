/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UsernameLinkHandle(
    @Schema(description = "A handle that can be included in username links to retrieve the stored encrypted username")
    @NotNull
    UUID usernameLinkHandle) {
}
