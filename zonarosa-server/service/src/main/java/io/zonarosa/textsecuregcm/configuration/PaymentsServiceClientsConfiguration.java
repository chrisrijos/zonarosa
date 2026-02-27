/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.http.HttpClient;
import java.util.Map;
import io.zonarosa.server.configuration.secrets.SecretString;
import io.zonarosa.server.currency.CoinGeckoClient;
import io.zonarosa.server.currency.FixerClient;

@JsonTypeName("default")
public record PaymentsServiceClientsConfiguration(@NotNull SecretString coinGeckoApiKey,
                                                  @NotNull SecretString fixerApiKey,
                                                  @NotEmpty Map<@NotBlank String, String> coinGeckoCurrencyIds) implements
    PaymentsServiceClientsFactory {

  @Override
  public FixerClient buildFixerClient(final HttpClient httpClient) {
    return new FixerClient(httpClient, fixerApiKey.value());
  }

  @Override
  public CoinGeckoClient buildCoinGeckoClient(final HttpClient httpClient) {
    return new CoinGeckoClient(httpClient, coinGeckoApiKey.value(), coinGeckoCurrencyIds);
  }
}
