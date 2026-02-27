/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class MessageCacheConfiguration {

  @JsonProperty
  @NotNull
  @Valid
  private FaultTolerantRedisClusterFactory cluster;

  @JsonProperty
  private int persistDelayMinutes = 10;

  public FaultTolerantRedisClusterFactory getRedisClusterConfiguration() {
    return cluster;
  }

  public int getPersistDelayMinutes() {
    return persistDelayMinutes;
  }
}
