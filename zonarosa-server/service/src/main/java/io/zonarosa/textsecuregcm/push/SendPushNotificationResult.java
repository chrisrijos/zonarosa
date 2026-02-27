/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.push;

import java.time.Instant;
import java.util.Optional;

public record SendPushNotificationResult(boolean accepted,
                                         Optional<String> errorCode,
                                         boolean unregistered,
                                         Optional<Instant> unregisteredTimestamp) {
}
