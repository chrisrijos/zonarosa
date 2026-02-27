//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import java.io.IOException;
import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * Error thrown by an unsuccessful registration request.
 *
 * <p>This is the parent class of more specific errors encountered as the result of sending a
 * registration request. It is also used for errors that don't require specialized client handling
 * or that aren't recognized error types.
 */
public class RegistrationException extends IOException {
  @CalledFromNative
  public RegistrationException(String message) {
    super(message);
  }
}
