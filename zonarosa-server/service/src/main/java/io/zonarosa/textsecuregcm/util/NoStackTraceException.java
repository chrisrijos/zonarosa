/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

/**
 * An abstract base class for exceptions that do not include a stack trace. Stackless exceptions are generally intended
 * for internal error-handling cases where the error will never be logged or otherwise reported.
 */
public abstract class NoStackTraceException extends Exception {

  public NoStackTraceException() {
    super(null, null, true, false);
  }

  public NoStackTraceException(final String message) {
    super(message, null, true, false);
  }

  public NoStackTraceException(final String message, final Throwable cause) {
    super(message, cause, true, false);
  }

  public NoStackTraceException(final Throwable cause) {
    super(null, cause, true, false);
  }
}
