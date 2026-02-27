/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import java.util.Random;
import io.zonarosa.server.storage.Device;

public class DevicesHelper {

  private static final Random RANDOM = new Random();

  public static Device createDevice(final byte deviceId) {
    return createDevice(deviceId, 0);
  }

  public static Device createDevice(final byte deviceId, final long lastSeen) {
    return createDevice(deviceId, lastSeen, 0);
  }

  public static Device createDevice(final byte deviceId, final long lastSeen, final int registrationId) {
    final Device device = new Device();
    device.setId(deviceId);
    device.setLastSeen(lastSeen);
    device.setUserAgent("OWT");
    device.setRegistrationId(registrationId);

    return device;
  }
}
