/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionForbiddenException extends SubscriptionException {

  public SubscriptionForbiddenException(final String message) {
    super(null, message);
  }
}
