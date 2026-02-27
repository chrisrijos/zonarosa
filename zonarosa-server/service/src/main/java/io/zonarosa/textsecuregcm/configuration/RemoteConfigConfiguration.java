/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RemoteConfigConfiguration(@NotNull Map<String, String> globalConfig) {

}
