//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * Thrown when verification fails for registration request with a recovery password.
 *
 * <p>When the websocket transport is in use, this corresponds to a {@code HTTP 403} response to a
 * POST request to {@code /v1/registration}.
 */
public class RegistrationRecoveryFailedException extends RegistrationException {
  @CalledFromNative
  private RegistrationRecoveryFailedException(String message) {
    super(message);
  }
}
