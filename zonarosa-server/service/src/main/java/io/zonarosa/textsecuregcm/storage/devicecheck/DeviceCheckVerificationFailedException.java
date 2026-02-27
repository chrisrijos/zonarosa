/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.storage.devicecheck;

public class DeviceCheckVerificationFailedException extends Exception {

  public DeviceCheckVerificationFailedException(Exception cause) {
    super(cause);
  }

  public DeviceCheckVerificationFailedException(String s) {
    super(s);
  }
}
