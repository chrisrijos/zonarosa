//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/** Unexpected response from the server. */
public class UnexpectedResponseException extends ChatServiceException {
  @CalledFromNative
  public UnexpectedResponseException(String message) {
    super(message);
  }
}
