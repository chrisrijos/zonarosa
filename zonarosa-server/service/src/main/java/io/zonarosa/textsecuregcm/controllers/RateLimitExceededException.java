/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.controllers;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
import io.grpc.StatusRuntimeException;
import io.zonarosa.server.grpc.ConvertibleToGrpcStatus;
import io.zonarosa.server.grpc.GrpcExceptions;

public class RateLimitExceededException extends Exception implements ConvertibleToGrpcStatus {

  @Nullable
  private final Duration retryDuration;

  /**
   * Constructs a new exception indicating when it may become safe to retry
   *
   * @param retryDuration A duration to wait before retrying, null if no duration can be indicated
   */
  public RateLimitExceededException(@Nullable final Duration retryDuration) {
    super(null, null, true, false);
    this.retryDuration = retryDuration;
  }

  public Optional<Duration> getRetryDuration() {
    return Optional.ofNullable(retryDuration);
  }

  @Override
  public StatusRuntimeException toStatusRuntimeException() {
    return GrpcExceptions.rateLimitExceeded(retryDuration);
  }
}
