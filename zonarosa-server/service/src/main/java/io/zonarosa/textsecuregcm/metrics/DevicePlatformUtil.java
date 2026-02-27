/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.metrics;

import org.apache.commons.lang3.StringUtils;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.util.ua.ClientPlatform;
import java.util.Optional;

public class DevicePlatformUtil {

  private DevicePlatformUtil() {
  }

  /**
   * Returns the most likely client platform for a device.
   *
   * @param device the device for which to find a client platform
   *
   * @return the most likely client platform for the given device or empty if no likely platform could be determined
   */
  public static Optional<ClientPlatform> getDevicePlatform(final Device device) {
    final Optional<ClientPlatform> clientPlatform;

    if (StringUtils.isNotBlank(device.getGcmId())) {
      clientPlatform = Optional.of(ClientPlatform.ANDROID);
    } else if (StringUtils.isNotBlank(device.getApnId())) {
      clientPlatform = Optional.of(ClientPlatform.IOS);
    } else {
      clientPlatform = Optional.empty();
    }

    return clientPlatform.or(() -> Optional.ofNullable(
        switch (device.getUserAgent()) {
          case "OWA" -> ClientPlatform.ANDROID;
          case "OWI", "OWP" -> ClientPlatform.IOS;
          case "OWD" -> ClientPlatform.DESKTOP;
          case null, default -> null;
        }));
  }
}
