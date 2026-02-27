/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.dynamic;

import com.vdurmont.semver4j.Semver;
import java.util.Map;
import io.zonarosa.server.util.ua.ClientPlatform;

public record DynamicRestDeprecationConfiguration(Map<ClientPlatform, PlatformConfiguration> platforms) {
  public record PlatformConfiguration(Semver minimumRestFreeVersion, int universalRolloutPercent) {}
}
