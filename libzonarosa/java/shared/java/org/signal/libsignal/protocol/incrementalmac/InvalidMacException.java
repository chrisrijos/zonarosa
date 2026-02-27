//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.incrementalmac;

import java.io.IOException;

public class InvalidMacException extends IOException {
  InvalidMacException() {
    super();
  }

  InvalidMacException(String message) {
    super(message);
  }
}
