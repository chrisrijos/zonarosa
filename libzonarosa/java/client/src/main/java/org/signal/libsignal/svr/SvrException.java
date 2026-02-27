//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.svr;

public class SvrException extends Exception {
  public SvrException(String message) {
    super(message);
  }

  public SvrException(String message, Throwable cause) {
    super(message, cause);
  }
}
