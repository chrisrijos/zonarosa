/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.metrics;

import javax.annotation.Nullable;
import java.util.Optional;

public class CallQualityInvalidArgumentsException extends Exception {
  private final @Nullable String field;

  public CallQualityInvalidArgumentsException(final String message) {
    this(message, null);
  }

  public CallQualityInvalidArgumentsException(final String message, final String field) {
    super(message);
    this.field = field;
  }

  public Optional<String> getField() {
    return Optional.ofNullable(field);
  }
}
