/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.zonarosa.server.identity.IdentityType;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.util.ByteArrayAdapter;
import io.zonarosa.server.util.ByteArrayBase64WithPaddingAdapter;

public record DeviceInfo(long id,

                         @JsonSerialize(using = ByteArrayBase64WithPaddingAdapter.Serializing.class)
                         @JsonDeserialize(using = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
                         byte[] name,

                         long lastSeen,

                         @Schema(description = "The registration ID of the given device.")
                         int registrationId,

                         @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
                         @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
                         @Schema(description = """
                             The ciphertext of the time in milliseconds since epoch when the device was attached
                             to the parent account, encoded in standard base64 without padding.
                             """)
                         byte[] createdAtCiphertext) {

  public static DeviceInfo forDevice(final Device device) {
    return new DeviceInfo(device.getId(), device.getName(), device.getLastSeen(), device.getRegistrationId(
        IdentityType.ACI), device.getCreatedAtCiphertext());
  }
}
