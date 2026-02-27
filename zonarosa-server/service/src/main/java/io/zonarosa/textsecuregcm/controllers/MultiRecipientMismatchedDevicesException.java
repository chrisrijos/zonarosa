/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import java.util.Map;
import io.zonarosa.server.identity.ServiceIdentifier;

public class MultiRecipientMismatchedDevicesException extends Exception {

  private final Map<ServiceIdentifier, MismatchedDevices> mismatchedDevicesByServiceIdentifier;

  public MultiRecipientMismatchedDevicesException(
      final Map<ServiceIdentifier, MismatchedDevices> mismatchedDevicesByServiceIdentifier) {

    if (mismatchedDevicesByServiceIdentifier.isEmpty()) {
      throw new IllegalArgumentException("Must provide non-empty map of service identifiers to mismatched devices");
    }

    this.mismatchedDevicesByServiceIdentifier = mismatchedDevicesByServiceIdentifier;
  }

  public Map<ServiceIdentifier, MismatchedDevices> getMismatchedDevicesByServiceIdentifier() {
    return mismatchedDevicesByServiceIdentifier;
  }
}
