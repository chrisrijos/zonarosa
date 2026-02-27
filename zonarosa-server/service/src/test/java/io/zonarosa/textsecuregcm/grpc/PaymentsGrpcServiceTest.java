/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static io.zonarosa.server.grpc.GrpcTestUtils.assertStatusException;

import io.grpc.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import io.zonarosa.chat.payments.GetCurrencyConversionsRequest;
import io.zonarosa.chat.payments.GetCurrencyConversionsResponse;
import io.zonarosa.chat.payments.PaymentsGrpc;
import io.zonarosa.server.currency.CurrencyConversionManager;
import io.zonarosa.server.entities.CurrencyConversionEntity;
import io.zonarosa.server.entities.CurrencyConversionEntityList;

class PaymentsGrpcServiceTest extends SimpleBaseGrpcTest<PaymentsGrpcService, PaymentsGrpc.PaymentsBlockingStub> {

  @Mock
  private CurrencyConversionManager currencyManager;

  @Override
  protected PaymentsGrpcService createServiceBeforeEachTest() {
    return new PaymentsGrpcService(currencyManager);
  }

  @Test
  void testGetCurrencyConversions() {
    final long timestamp = System.currentTimeMillis();
    when(currencyManager.getCurrencyConversions()).thenReturn(Optional.of(
        new CurrencyConversionEntityList(List.of(
            new CurrencyConversionEntity("FOO", Map.of(
                "USD", new BigDecimal("2.35"),
                "EUR", new BigDecimal("1.89")
            )),
            new CurrencyConversionEntity("BAR", Map.of(
                "USD", new BigDecimal("1.50"),
                "EUR", new BigDecimal("0.98")
            ))
        ), timestamp)));

    final GetCurrencyConversionsResponse currencyConversions = authenticatedServiceStub().getCurrencyConversions(
        GetCurrencyConversionsRequest.newBuilder().build());

    assertEquals(timestamp, currencyConversions.getTimestamp());
    assertEquals(2, currencyConversions.getCurrenciesCount());
    assertEquals("FOO", currencyConversions.getCurrencies(0).getBase());
    assertEquals("2.35", currencyConversions.getCurrencies(0).getConversionsMap().get("USD"));
  }

  @Test
  void testUnavailable() {
    when(currencyManager.getCurrencyConversions()).thenReturn(Optional.empty());
    assertStatusException(Status.UNAVAILABLE, () -> authenticatedServiceStub().getCurrencyConversions(
        GetCurrencyConversionsRequest.newBuilder().build()));
  }
}
