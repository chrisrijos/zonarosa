/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.zonarosa.server.storage.DeviceCapability;

public class DeviceCapabilityUtil {

  private DeviceCapabilityUtil() {
  }

  public static DeviceCapability fromGrpcDeviceCapability(final io.zonarosa.chat.common.DeviceCapability grpcDeviceCapability) {
    return switch (grpcDeviceCapability) {
      case DEVICE_CAPABILITY_STORAGE -> DeviceCapability.STORAGE;
      case DEVICE_CAPABILITY_TRANSFER -> DeviceCapability.TRANSFER;
      case DEVICE_CAPABILITY_ATTACHMENT_BACKFILL -> DeviceCapability.ATTACHMENT_BACKFILL;
      case DEVICE_CAPABILITY_SPARSE_POST_QUANTUM_RATCHET -> DeviceCapability.SPARSE_POST_QUANTUM_RATCHET;
      case DEVICE_CAPABILITY_UNSPECIFIED, UNRECOGNIZED ->
          throw GrpcExceptions.invalidArguments("unrecognized device capability");
    };
  }

  public static io.zonarosa.chat.common.DeviceCapability toGrpcDeviceCapability(final DeviceCapability deviceCapability) {
    return switch (deviceCapability) {
      case STORAGE -> io.zonarosa.chat.common.DeviceCapability.DEVICE_CAPABILITY_STORAGE;
      case TRANSFER -> io.zonarosa.chat.common.DeviceCapability.DEVICE_CAPABILITY_TRANSFER;
      case ATTACHMENT_BACKFILL -> io.zonarosa.chat.common.DeviceCapability.DEVICE_CAPABILITY_ATTACHMENT_BACKFILL;
      case SPARSE_POST_QUANTUM_RATCHET -> io.zonarosa.chat.common.DeviceCapability.DEVICE_CAPABILITY_SPARSE_POST_QUANTUM_RATCHET;
    };
  }
}
