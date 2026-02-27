/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionNotFoundException extends SubscriptionException {

  public SubscriptionNotFoundException() {
    super(null);
  }

  public SubscriptionNotFoundException(Exception cause) {
    super(cause);
  }
}
