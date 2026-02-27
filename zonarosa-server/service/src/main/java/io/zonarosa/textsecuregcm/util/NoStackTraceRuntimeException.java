/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

/**
 * An abstract base class for runtime exceptions that do not include a stack trace. Stackless exceptions are generally
 * intended for internal error-handling cases where the error will never be logged or otherwise reported.
 */
public abstract class NoStackTraceRuntimeException extends RuntimeException {

  public NoStackTraceRuntimeException() {
    super(null, null, true, false);
  }

  public NoStackTraceRuntimeException(final String message) {
    super(message, null, true, false);
  }

  public NoStackTraceRuntimeException(final String message, final Throwable cause) {
    super(message, cause, true, false);
  }

  public NoStackTraceRuntimeException(final Throwable cause) {
    super(null, cause, true, false);
  }
}
