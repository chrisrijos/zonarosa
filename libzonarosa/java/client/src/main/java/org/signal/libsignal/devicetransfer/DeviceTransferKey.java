//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.devicetransfer;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;

public class DeviceTransferKey {
  byte[] keyMaterial;

  public DeviceTransferKey() {
    this.keyMaterial = Native.DeviceTransfer_GeneratePrivateKey();
  }

  public byte[] keyMaterial() {
    return this.keyMaterial;
  }

  public byte[] generateCertificate(String name, int daysTilExpires) {
    return filterExceptions(
        () -> Native.DeviceTransfer_GenerateCertificate(this.keyMaterial, name, daysTilExpires));
  }
}
