/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionPaymentRequiresActionException extends SubscriptionInvalidArgumentsException {

  public SubscriptionPaymentRequiresActionException(String message) {
    super(message, null);
  }

  public SubscriptionPaymentRequiresActionException() {
    super(null, null);
  }
}
