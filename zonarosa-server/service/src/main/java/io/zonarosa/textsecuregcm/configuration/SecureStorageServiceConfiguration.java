/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import javax.annotation.Nullable;
import io.zonarosa.server.configuration.secrets.SecretBytes;

public record SecureStorageServiceConfiguration(@NotNull SecretBytes userAuthenticationTokenSharedSecret,
                                                @NotBlank String uri,
                                                @NotEmpty List<@NotBlank String> storageCaCertificates,
                                                @Nullable String circuitBreakerConfigurationName,
                                                @Nullable String retryConfigurationName) {
}
