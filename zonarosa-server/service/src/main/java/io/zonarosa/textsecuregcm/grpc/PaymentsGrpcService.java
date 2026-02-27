/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import io.zonarosa.chat.payments.GetCurrencyConversionsRequest;
import io.zonarosa.chat.payments.GetCurrencyConversionsResponse;
import io.zonarosa.chat.payments.SimplePaymentsGrpc;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.currency.CurrencyConversionManager;
import io.zonarosa.server.entities.CurrencyConversionEntityList;

public class PaymentsGrpcService extends SimplePaymentsGrpc.PaymentsImplBase {

  private final CurrencyConversionManager currencyManager;


  public PaymentsGrpcService(final CurrencyConversionManager currencyManager) {
    this.currencyManager = requireNonNull(currencyManager);
  }

  @Override
  public GetCurrencyConversionsResponse getCurrencyConversions(final GetCurrencyConversionsRequest request) {
    AuthenticationUtil.requireAuthenticatedDevice();

    final CurrencyConversionEntityList currencyConversionEntityList = currencyManager
        .getCurrencyConversions()
        .orElseThrow(() -> GrpcExceptions.unavailable("currency conversions not available"));

    final List<GetCurrencyConversionsResponse.CurrencyConversionEntity> currencyConversionEntities = currencyConversionEntityList
        .getCurrencies()
        .stream()
        .map(cce -> GetCurrencyConversionsResponse.CurrencyConversionEntity.newBuilder()
            .setBase(cce.getBase())
            .putAllConversions(transformBigDecimalsToStrings(cce.getConversions()))
            .build())
        .toList();

    return GetCurrencyConversionsResponse.newBuilder()
        .addAllCurrencies(currencyConversionEntities).setTimestamp(currencyConversionEntityList.getTimestamp())
        .build();
  }

  @Nonnull
  private static Map<String, String> transformBigDecimalsToStrings(final Map<String, BigDecimal> conversions) {
    AuthenticationUtil.requireAuthenticatedDevice();
    return conversions.entrySet().stream()
        .map(e -> Pair.of(e.getKey(), e.getValue().toString()))
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }
}
