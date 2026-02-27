/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.server.util.IdentityKeyAdapter;

public class PreKeyResponse {

  @JsonProperty
  @JsonSerialize(using = IdentityKeyAdapter.Serializer.class)
  @JsonDeserialize(using = IdentityKeyAdapter.Deserializer.class)
  @Schema(description="the public identity key for the requested identity")
  private IdentityKey identityKey;

  @JsonProperty
  @Schema(description="information about each requested device")
  private List<PreKeyResponseItem> devices;

  public PreKeyResponse() {}

  public PreKeyResponse(IdentityKey identityKey, List<PreKeyResponseItem> devices) {
    this.identityKey = identityKey;
    this.devices = devices;
  }

  @VisibleForTesting
  public IdentityKey getIdentityKey() {
    return identityKey;
  }

  @VisibleForTesting
  @JsonIgnore
  public PreKeyResponseItem getDevice(byte deviceId) {
    for (PreKeyResponseItem device : devices) {
      if (device.getDeviceId() == deviceId) return device;
    }

    return null;
  }

  @VisibleForTesting
  @JsonIgnore
  public int getDevicesCount() {
    return devices.size();
  }

}
