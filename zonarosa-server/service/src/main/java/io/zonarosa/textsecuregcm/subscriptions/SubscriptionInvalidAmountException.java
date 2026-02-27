/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.subscriptions;

public class SubscriptionInvalidAmountException extends SubscriptionInvalidArgumentsException {

  private String errorCode;

  public SubscriptionInvalidAmountException(String errorCode) {
    super(null, null);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
