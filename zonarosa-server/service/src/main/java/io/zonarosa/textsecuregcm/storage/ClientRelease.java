/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import com.vdurmont.semver4j.Semver;
import io.zonarosa.server.util.ua.ClientPlatform;
import java.time.Instant;

public record ClientRelease(ClientPlatform platform, Semver version, Instant release, Instant expiration) {
}
