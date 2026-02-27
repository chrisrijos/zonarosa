/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import io.zonarosa.server.configuration.secrets.SecretBytes;
import io.zonarosa.server.configuration.secrets.SecretString;
import io.zonarosa.server.subscriptions.PaymentMethod;

public record StripeConfiguration(@NotNull SecretString apiKey,
                                  @NotNull SecretBytes idempotencyKeyGenerator,
                                  @NotBlank String boostDescription,
                                  @Valid @NotEmpty Map<PaymentMethod, Set<@NotBlank String>> supportedCurrenciesByPaymentMethod) {
}
