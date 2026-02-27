/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.grpc.validators;

public class FieldValidationException extends Exception {
  public FieldValidationException(String message) {
    super(message);
  }
}
