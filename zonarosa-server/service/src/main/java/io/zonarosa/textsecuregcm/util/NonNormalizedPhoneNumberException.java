/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

public class NonNormalizedPhoneNumberException extends Exception {

  private final String originalNumber;
  private final String normalizedNumber;

  public NonNormalizedPhoneNumberException(final String originalNumber, final String normalizedNumber) {
    this.originalNumber = originalNumber;
    this.normalizedNumber = normalizedNumber;
  }

  public String getOriginalNumber() {
    return originalNumber;
  }

  public String getNormalizedNumber() {
    return normalizedNumber;
  }
}
