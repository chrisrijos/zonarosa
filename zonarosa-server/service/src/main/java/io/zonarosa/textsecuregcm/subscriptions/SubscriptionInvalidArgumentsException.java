/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionInvalidArgumentsException extends SubscriptionException {

  public SubscriptionInvalidArgumentsException(final String message, final Exception cause) {
    super(cause, message);
  }

  public SubscriptionInvalidArgumentsException(final String message) {
    this(message, null);
  }
}
