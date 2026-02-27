//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * No session with the specified ID could be found.
 *
 * <p>When the websocket transport is in use, this corresponds to a {@code HTTP 404} response to
 * requests to endpoints with the {@code /v1/verification/session} prefix.
 */
public class RegistrationSessionNotFoundException extends RegistrationException {
  @CalledFromNative
  public RegistrationSessionNotFoundException(String message) {
    super(message);
  }
}
