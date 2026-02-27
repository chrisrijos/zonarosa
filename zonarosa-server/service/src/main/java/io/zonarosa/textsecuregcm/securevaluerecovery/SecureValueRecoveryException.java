/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.securevaluerecovery;

public class SecureValueRecoveryException extends RuntimeException {
  private final String statusCode;

  public SecureValueRecoveryException(final String message, final String statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public String getStatusCode() {
    return statusCode;
  }
}
