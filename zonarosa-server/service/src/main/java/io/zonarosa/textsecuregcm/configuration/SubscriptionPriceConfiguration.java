/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import io.zonarosa.server.subscriptions.PaymentProvider;

public record SubscriptionPriceConfiguration(@Valid @NotEmpty Map<PaymentProvider, @NotBlank String> processorIds,
                                             @NotNull @DecimalMin("0.01") BigDecimal amount) {

}
