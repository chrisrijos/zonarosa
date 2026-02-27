/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.telephony;

/// Indicates that a request for carrier data failed permanently (e.g. it was affirmatively rejected by the provider)
/// and should not be retried without modification.
public class CarrierDataException extends Exception {

  public CarrierDataException(final String message) {
    super(message);
  }
}
