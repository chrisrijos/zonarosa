/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import io.zonarosa.server.configuration.secrets.SecretString;

/**
 * @param credentialsJson  Service account credentials for Play Billing API
 * @param packageName      The app package name
 * @param applicationName  The app application name
 * @param productIdToLevel A map of productIds offered in the play billing subscription catalog to their corresponding
 *                         zonarosa subscription level
 */
public record GooglePlayBillingConfiguration(
    @NotBlank String credentialsJson,
    @NotNull String packageName,
    @NotBlank String applicationName,
    @NotNull Map<String, Long> productIdToLevel) {}
