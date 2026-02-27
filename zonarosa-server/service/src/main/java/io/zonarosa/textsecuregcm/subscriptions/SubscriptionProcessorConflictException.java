/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionProcessorConflictException extends SubscriptionException {

  public SubscriptionProcessorConflictException() {
    super(null, null);
  }

  public SubscriptionProcessorConflictException(final String message) {
    super(null, message);
  }
}
