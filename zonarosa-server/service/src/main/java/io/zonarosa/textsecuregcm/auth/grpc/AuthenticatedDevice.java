/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth.grpc;

import java.util.UUID;

public record AuthenticatedDevice(UUID accountIdentifier, byte deviceId) {
}
