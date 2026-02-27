/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record StaleDevicesResponse(@JsonProperty
                                   @Schema(description = "Devices that are no longer active")
                                   Set<Byte> staleDevices) {
}
