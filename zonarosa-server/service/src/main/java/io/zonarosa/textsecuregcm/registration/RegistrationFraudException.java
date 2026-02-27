/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.registration;

public class RegistrationFraudException extends Exception {
  public RegistrationFraudException(final RegistrationServiceSenderException cause) {
    super(null, cause, true, false);
  }
}
