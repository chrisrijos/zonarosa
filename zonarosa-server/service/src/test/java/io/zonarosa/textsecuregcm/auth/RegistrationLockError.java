/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

public enum RegistrationLockError {
  MISMATCH(RegistrationLockVerificationManager.FAILURE_HTTP_STATUS),
  RATE_LIMITED(429)
  ;

  private final int expectedStatus;

  RegistrationLockError(final int expectedStatus) {
    this.expectedStatus = expectedStatus;
  }

  public int getExpectedStatus() {
    return expectedStatus;
  }
}
