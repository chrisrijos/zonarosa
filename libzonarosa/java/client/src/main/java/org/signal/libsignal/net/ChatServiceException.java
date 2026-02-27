//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import java.io.IOException;

/** Error thrown by Chat Service API. */
public class ChatServiceException extends IOException {
  public ChatServiceException(String message) {
    super(message);
  }

  public ChatServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
