/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.entities;

import jakarta.validation.constraints.NotEmpty;

public record GcmRegistrationId(@NotEmpty String gcmRegistrationId) {
}
