/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import io.zonarosa.server.configuration.secrets.SecretBytes;

public record PaymentsServiceConfiguration(@NotNull SecretBytes userAuthenticationTokenSharedSecret,
                                           @NotEmpty List<String> paymentCurrencies,
                                           @NotNull @Valid PaymentsServiceClientsFactory externalClients) {


}
