/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotNull;

public record GrpcConfiguration(@NotNull String bindAddress, @NotNull Integer port) {
  public GrpcConfiguration {
    if (bindAddress == null || bindAddress.isEmpty()) {
      bindAddress = "localhost";
    }
  }
}
