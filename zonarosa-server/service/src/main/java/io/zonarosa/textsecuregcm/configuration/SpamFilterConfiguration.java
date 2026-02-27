/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SpamFilterConfiguration {

  @JsonProperty
  @NotBlank
  private final String environment;

  @JsonCreator
  public SpamFilterConfiguration(@JsonProperty("environment") final String environment) {
    this.environment = environment;
  }

  public String getEnvironment() {
    return environment;
  }
}
