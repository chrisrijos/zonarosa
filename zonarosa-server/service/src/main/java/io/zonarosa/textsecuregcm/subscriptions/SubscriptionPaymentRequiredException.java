/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionPaymentRequiredException extends SubscriptionException {

  public SubscriptionPaymentRequiredException() {
    super(null, null);
  }

  public SubscriptionPaymentRequiredException(String message) {
    super(null, message);
  }
}
