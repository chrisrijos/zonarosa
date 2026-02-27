/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

/**
 * Attempted to retrieve a receipt for a subscription that hasn't yet been charged or the invoice is in the open state
 */
public class SubscriptionReceiptRequestedForOpenPaymentException extends SubscriptionException {

  public SubscriptionReceiptRequestedForOpenPaymentException() {
    super(null, null);
  }
}
