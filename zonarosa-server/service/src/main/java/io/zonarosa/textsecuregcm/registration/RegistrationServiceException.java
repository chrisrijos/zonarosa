/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.registration;

import java.util.Optional;
import io.zonarosa.server.entities.RegistrationServiceSession;

/**
 * When the Registration Service returns an error, it will also return the latest {@link RegistrationServiceSession}
 * data, so that clients may have the latest details on requesting and submitting codes.
 */
public class RegistrationServiceException extends Exception {

  private final RegistrationServiceSession registrationServiceSession;

  public RegistrationServiceException(final RegistrationServiceSession registrationServiceSession) {
    super(null, null, true, false);
    this.registrationServiceSession = registrationServiceSession;
  }

  /**
   * @return if empty, the session that encountered should be considered non-existent and may be discarded
   */
  public Optional<RegistrationServiceSession> getRegistrationSession() {
    return Optional.ofNullable(registrationServiceSession);
  }
}
