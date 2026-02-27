/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.integration.config;

import jakarta.validation.constraints.NotBlank;

public record DynamoDbTables(@NotBlank String registrationRecovery,
                             @NotBlank String verificationSessions,
                             @NotBlank String phoneNumberIdentifiers) {
}
