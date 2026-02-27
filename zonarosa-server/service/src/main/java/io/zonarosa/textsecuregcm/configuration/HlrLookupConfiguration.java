/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import io.zonarosa.server.configuration.secrets.SecretString;
import javax.annotation.Nullable;

public record HlrLookupConfiguration(SecretString apiKey,
                                     SecretString apiSecret,
                                     @Nullable String circuitBreakerConfigurationName,
                                     @Nullable String retryConfigurationName) {
}
