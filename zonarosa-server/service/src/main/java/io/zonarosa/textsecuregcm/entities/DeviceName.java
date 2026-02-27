/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.zonarosa.server.util.ByteArrayAdapter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record DeviceName(@JsonSerialize(using = ByteArrayAdapter.Serializing.class)
                         @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
                         @NotEmpty
                         @Size(max = 225)
                         byte[] deviceName) {
}
