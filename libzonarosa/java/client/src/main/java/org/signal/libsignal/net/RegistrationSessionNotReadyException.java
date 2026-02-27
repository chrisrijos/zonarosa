//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * The session is already verified or not in a state to request a code because requested information
 * hasn't been provided yet.
 *
 * <p>When the websocket transport is in use, this corresponds to a {@code HTTP 409} response to a
 * POST request to {@code /v1/verification/session/{sessionId}/code}.
 */
public class RegistrationSessionNotReadyException extends RegistrationException {
  @CalledFromNative
  public RegistrationSessionNotReadyException(String message) {
    super(message);
  }
}
