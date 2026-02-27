/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

public class ReportMessageConfiguration {

  @JsonProperty
  @NotNull
  private final Duration reportTtl = Duration.ofDays(7);

  @JsonProperty
  @NotNull
  private final Duration counterTtl = Duration.ofDays(1);

  public Duration getReportTtl() {
    return reportTtl;
  }

  public Duration getCounterTtl() {
    return counterTtl;
  }
}
