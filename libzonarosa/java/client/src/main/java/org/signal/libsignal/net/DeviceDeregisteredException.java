//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/** Indicates that the local device has been deregistered or delinked. */
public class DeviceDeregisteredException extends ChatServiceException {
  public DeviceDeregisteredException(String message) {
    super(message);
  }
}
