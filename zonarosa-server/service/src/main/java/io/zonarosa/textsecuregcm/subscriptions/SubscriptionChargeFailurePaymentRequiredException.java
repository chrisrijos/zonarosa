/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionChargeFailurePaymentRequiredException extends SubscriptionPaymentRequiredException {

  private final PaymentProvider processor;
  private final ChargeFailure chargeFailure;

  public SubscriptionChargeFailurePaymentRequiredException(final PaymentProvider processor,
      final ChargeFailure chargeFailure) {
    super();
    this.processor = processor;
    this.chargeFailure = chargeFailure;
  }

  public PaymentProvider getProcessor() {
    return processor;
  }

  public ChargeFailure getChargeFailure() {
    return chargeFailure;
  }

}
