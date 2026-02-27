/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

public record ClientReleaseConfiguration(@NotNull Duration refreshInterval) {
}
