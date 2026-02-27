/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import java.util.Set;

public record MismatchedDevices(Set<Byte> missingDeviceIds, Set<Byte> extraDeviceIds, Set<Byte> staleDeviceIds) {

  public MismatchedDevices {
    if (missingDeviceIds.isEmpty() && extraDeviceIds.isEmpty() && staleDeviceIds.isEmpty()) {
      throw new IllegalArgumentException("At least one of missingDevices, extraDevices, or staleDevices must be non-empty");
    }
  }
}
