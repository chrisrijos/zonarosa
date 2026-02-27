/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.zonarosa.server.configuration.secrets.SecretString;


public record ApnConfiguration(@NotNull SecretString teamId,
                               @NotNull SecretString keyId,
                               @NotNull SecretString signingKey,
                               @NotBlank String bundleId,
                               boolean sandbox) {
}
