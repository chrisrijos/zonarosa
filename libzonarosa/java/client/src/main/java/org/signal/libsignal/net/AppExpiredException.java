//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/** Indicates that the local application is too old, and was rejected by the server. */
public class AppExpiredException extends ChatServiceException {
  public AppExpiredException(String message) {
    super(message);
  }
}
