/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import io.zonarosa.server.subscriptions.PaymentProvider;
import io.zonarosa.server.util.EnumMapUtil;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

public class IssuedReceiptsTableConfiguration extends DynamoDbTables.TableWithExpiration {

  private final byte[] generator;

  /**
   * The maximum number of issued receipts the issued receipt manager should issue for a particular itemId
   */
  private final EnumMap<PaymentProvider, Integer> maxIssuedReceiptsPerPaymentId;

  public IssuedReceiptsTableConfiguration(
      @JsonProperty("tableName") final String tableName,
      @JsonProperty("expiration") final Duration expiration,
      @JsonProperty("generator") final byte[] generator,
      @JsonProperty("maxIssuedReceiptsPerPaymentId") final Map<PaymentProvider, Integer> maxIssuedReceiptsPerPaymentId) {
    super(tableName, expiration);
    this.generator = generator;
    this.maxIssuedReceiptsPerPaymentId = EnumMapUtil.toCompleteEnumMap(PaymentProvider.class, maxIssuedReceiptsPerPaymentId);
  }

  @NotEmpty
  public byte[] getGenerator() {
    return generator;
  }

  public EnumMap<PaymentProvider, Integer> getmaxIssuedReceiptsPerPaymentId() {
    return maxIssuedReceiptsPerPaymentId;
  }
}
