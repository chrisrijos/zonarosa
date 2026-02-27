//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/**
 * Indicates that the same credentials we used to open an authenticated ChatConnection were also
 * used to open a second connection "elsewhere".
 */
public class ConnectedElsewhereException extends ChatServiceException {
  public ConnectedElsewhereException(String message) {
    super(message);
  }
}
