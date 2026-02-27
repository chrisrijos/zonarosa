/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import io.zonarosa.server.currency.CoinGeckoClient;
import io.zonarosa.server.currency.FixerClient;
import java.net.http.HttpClient;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = PaymentsServiceClientsConfiguration.class)
public interface PaymentsServiceClientsFactory extends Discoverable {

  FixerClient buildFixerClient(final HttpClient httpClient);

  CoinGeckoClient buildCoinGeckoClient(HttpClient httpClient);
}
