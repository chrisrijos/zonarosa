//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/** Error thrown by a failed CDSI lookup operation. */
public class CdsiInvalidTokenException extends Exception {
  public CdsiInvalidTokenException(String message) {
    super(message);
  }
}
