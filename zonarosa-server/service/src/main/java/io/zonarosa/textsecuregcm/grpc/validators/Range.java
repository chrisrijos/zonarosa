/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc.validators;

public record Range(long min, long max) {
  public Range {
    if (min > max) {
      throw new IllegalArgumentException("invalid range values: expected min <= max but have [%d, %d],".formatted(min, max));
    }
  }
}
