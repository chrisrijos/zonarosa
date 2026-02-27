/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.zonarosa.server.util.ValidBase64URLString;

public record RemoteAttachment(
    @Schema(description = "The attachment cdn")
    @NotNull
    Integer cdn,

    @NotBlank
    @ValidBase64URLString
    @Size(max = 64)
    @Schema(description = "The attachment key", maxLength = 64)
    String key) implements TransferArchiveResult {}
