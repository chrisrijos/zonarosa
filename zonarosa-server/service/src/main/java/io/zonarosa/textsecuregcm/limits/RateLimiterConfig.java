/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.limits;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;

public record RateLimiterConfig(long bucketSize, Duration permitRegenerationDuration, boolean failOpen) {

  public double leakRatePerMillis() {
    return 1.0 / (permitRegenerationDuration.toNanos() / 1e6);
  }

  @AssertTrue
  @Schema(hidden = true)
  public boolean isPositiveRegenerationRate() {
    try {
      return permitRegenerationDuration.toNanos() > 0;
    } catch (final ArithmeticException e) {
      // The duration was too large to fit in a long, so it's definitely positive
      return true;
    }
  }
}
