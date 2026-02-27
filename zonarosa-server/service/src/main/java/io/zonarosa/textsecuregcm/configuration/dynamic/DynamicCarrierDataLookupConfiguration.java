/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.dynamic;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

public record DynamicCarrierDataLookupConfiguration(boolean enabled, @NotNull Duration maxCacheAge) {

  public static Duration DEFAULT_MAX_CACHE_AGE = Duration.ofDays(7);

  public DynamicCarrierDataLookupConfiguration() {
    this(false, DEFAULT_MAX_CACHE_AGE);
  }

  public DynamicCarrierDataLookupConfiguration {
    if (maxCacheAge == null) {
      maxCacheAge = DEFAULT_MAX_CACHE_AGE;
    }
  }
}
