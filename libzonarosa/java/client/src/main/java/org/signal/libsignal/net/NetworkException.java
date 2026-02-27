//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import java.io.IOException;

/** Error thrown by a low-level network failure, for example failure to open a TCP connection. */
public class NetworkException extends IOException {
  public NetworkException(String message) {
    super(message);
  }
}
