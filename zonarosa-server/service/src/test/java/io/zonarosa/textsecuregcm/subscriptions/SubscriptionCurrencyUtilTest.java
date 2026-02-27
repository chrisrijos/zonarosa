/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

import com.google.api.services.androidpublisher.model.Money;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionCurrencyUtilTest {

  @Test
  void convertGoogleMoneyToApiAmount() {
    Money money = new Money();
    money.setCurrencyCode("USD");
    money.setUnits(4L);

    BigDecimal amt = SubscriptionCurrencyUtil.convertGoogleMoneyToApiAmount(money);
    Assertions.assertThat(amt).isCloseTo(BigDecimal.valueOf(400), Percentage.withPercentage(0.0001));

    money.setNanos(990000000);
    amt = SubscriptionCurrencyUtil.convertGoogleMoneyToApiAmount(money);
    Assertions.assertThat(amt).isCloseTo(BigDecimal.valueOf(499), Percentage.withPercentage(0.0001));

  }
}
