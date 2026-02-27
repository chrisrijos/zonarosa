//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * The session ID is not valid.
 *
 * <p>Thrown when attempting to make a request, or when a response is received with a structurally
 * invalid validation session ID.
 */
public class RegistrationSessionIdInvalidException extends RegistrationException {
  @CalledFromNative
  public RegistrationSessionIdInvalidException(String message) {
    super(message);
  }
}
