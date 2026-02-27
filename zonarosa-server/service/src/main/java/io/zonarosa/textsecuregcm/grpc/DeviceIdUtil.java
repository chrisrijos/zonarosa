/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.Status;
import io.zonarosa.server.storage.Device;

public class DeviceIdUtil {

  public static boolean isValid(int deviceId) {
    return deviceId >= Device.PRIMARY_ID && deviceId <= Byte.MAX_VALUE;
  }

  static byte validate(int deviceId) {
    if (!isValid(deviceId)) {
      throw GrpcExceptions.invalidArguments("device ID is out of range");
    }

    return (byte) deviceId;
  }
}
