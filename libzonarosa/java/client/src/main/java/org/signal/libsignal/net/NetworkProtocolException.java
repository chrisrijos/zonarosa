//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import java.io.IOException;

/**
 * Error thrown by a network failure on a higher level, for example failure to establish a WebSocket
 * connection.
 */
public class NetworkProtocolException extends IOException {
  public NetworkProtocolException(String message) {
    super(message);
  }
}
