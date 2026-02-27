/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.zonarosa.server.configuration.secrets.SecretString;

public record KeyTransparencyServiceConfiguration(@NotBlank String host,
                                                  @Positive int port,
                                                  @NotBlank String tlsCertificate,
                                                  @NotBlank String clientCertificate,
                                                  @NotNull SecretString clientPrivateKey) {}
