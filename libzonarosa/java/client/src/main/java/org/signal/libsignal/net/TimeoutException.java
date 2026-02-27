//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/** Request timed out. */
public class TimeoutException extends ChatServiceException {
  @CalledFromNative
  public TimeoutException(String message) {
    super(message);
  }
}
