/*
 * Copyright 2013-2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util.ua;

import com.vdurmont.semver4j.Semver;
import javax.annotation.Nullable;
import java.util.Objects;

public record UserAgent(ClientPlatform platform, Semver version, @Nullable String additionalSpecifiers) {
}
