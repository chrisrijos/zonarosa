/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.validation.constraints.Min;
import java.time.Duration;

public class RetryConfiguration {

  @JsonProperty
  @Min(1)
  private int maxAttempts = 3;

  @JsonProperty
  @Min(1)
  private long waitDuration = RetryConfig.DEFAULT_WAIT_DURATION;

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(final int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getWaitDuration() {
    return waitDuration;
  }

  public void setWaitDuration(final long waitDuration) {
    this.waitDuration = waitDuration;
  }

  public <T> RetryConfig.Builder<T> toRetryConfigBuilder() {
    return RetryConfig.<T>custom()
        .maxAttempts(getMaxAttempts())
        .waitDuration(Duration.ofMillis(getWaitDuration()));
  }
}
