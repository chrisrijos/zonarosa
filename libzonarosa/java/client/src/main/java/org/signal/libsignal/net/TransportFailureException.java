//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/** Transport-level error in Chat Service communication. */
public class TransportFailureException extends ChatServiceException {
  @CalledFromNative
  public TransportFailureException(String message) {
    super(message);
  }
}
