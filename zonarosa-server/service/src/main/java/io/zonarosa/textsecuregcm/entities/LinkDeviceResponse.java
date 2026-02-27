/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import java.util.UUID;

public record LinkDeviceResponse(UUID uuid, UUID pni, byte deviceId) {
}
