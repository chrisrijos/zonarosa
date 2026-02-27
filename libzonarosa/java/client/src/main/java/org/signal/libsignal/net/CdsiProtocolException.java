//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/** Error thrown when a CDSI server returns an unexpected response. */
public class CdsiProtocolException extends Exception {
  private CdsiProtocolException(String message) {
    super(message);
  }
}
