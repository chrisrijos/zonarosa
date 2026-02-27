/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import io.zonarosa.server.configuration.secrets.SecretBytes;
import io.zonarosa.server.util.ExactlySize;
import javax.annotation.Nullable;

public record SecureValueRecoveryConfiguration(
    @NotBlank String uri,
    @ExactlySize(32) SecretBytes userAuthenticationTokenSharedSecret,
    @ExactlySize(32) SecretBytes userIdTokenSharedSecret,
    @NotEmpty List<@NotBlank String> svrCaCertificates,
    @Nullable String circuitBreakerConfigurationName,
    @Nullable String retryConfigurationName) {
}
