/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import io.zonarosa.server.configuration.secrets.SecretString;
import io.zonarosa.server.subscriptions.PaymentMethod;

/**
 * @param merchantId          the Braintree merchant ID
 * @param publicKey           the Braintree API public key
 * @param privateKey          the Braintree API private key
 * @param environment         the Braintree environment ("production" or "sandbox")
 * @param supportedCurrenciesByPaymentMethod the set of supported currencies
 * @param graphqlUrl          the Braintree GraphQL URl to use (this must match the environment)
 * @param merchantAccounts    merchant account within the merchant for processing individual currencies
 * @param circuitBreakerConfigurationName the name of the circuit breaker configuration for the breaker used by the
 *                                        GraphQL HTTP client; if `null`, uses the global default configuration
 */
public record BraintreeConfiguration(@NotBlank String merchantId,
                                     @NotNull SecretString publicKey,
                                     @NotNull SecretString privateKey,
                                     @NotBlank String environment,
                                     @Valid @NotEmpty Map<PaymentMethod, Set<@NotBlank String>> supportedCurrenciesByPaymentMethod,
                                     @NotBlank String graphqlUrl,
                                     @NotEmpty Map<String, String> merchantAccounts,
                                     @Nullable String circuitBreakerConfigurationName,
                                     @Valid @NotNull PubSubPublisherFactory pubSubPublisher) {
}
