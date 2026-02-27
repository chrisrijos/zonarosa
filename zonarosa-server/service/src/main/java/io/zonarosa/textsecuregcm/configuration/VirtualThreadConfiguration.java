/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import java.time.Duration;

public record VirtualThreadConfiguration(
    Duration pinEventThreshold,
    Integer maxConcurrentThreadsPerExecutor) {

  public VirtualThreadConfiguration() {
    this(null, null);
  }

  public VirtualThreadConfiguration {
    if (maxConcurrentThreadsPerExecutor == null) {
      maxConcurrentThreadsPerExecutor = 1_000_000;
    }
    if (pinEventThreshold == null) {
      pinEventThreshold = Duration.ofMillis(1);
    }
  }
}
