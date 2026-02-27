//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.svr;

public class InvalidSvrBDataException extends SvrException {
  public InvalidSvrBDataException(String message) {
    super(message);
  }

  public InvalidSvrBDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
