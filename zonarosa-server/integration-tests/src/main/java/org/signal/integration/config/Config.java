/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.integration.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.zonarosa.server.configuration.DynamoDbClientFactory;

public record Config(@NotBlank String domain,
                     @NotBlank String rootCert,
                     @NotNull @Valid DynamoDbClientFactory dynamoDbClient,
                     @NotNull @Valid DynamoDbTables dynamoDbTables,
                     @NotBlank String prescribedRegistrationNumber,
                     @NotBlank String prescribedRegistrationCode) {
}
