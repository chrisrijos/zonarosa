//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * The request to send a verification code with the given transport could not be fulfilled, but may
 * succeed with a different transport.
 *
 * <p>When the websocket transport is in use, this corresponds to a {@code HTTP 418} response to a
 * POST request to {@code /v1/verification/session/{sessionId}/code}.
 */
@CalledFromNative
public class RegistrationSessionSendCodeException extends RegistrationException {
  public RegistrationSessionSendCodeException(String message) {
    super(message);
  }
}
