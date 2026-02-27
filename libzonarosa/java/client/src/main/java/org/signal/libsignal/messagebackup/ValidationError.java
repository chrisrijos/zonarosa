//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.messagebackup;

/**
 * Error from validating a message backup bundle.
 *
 * <p>{@link Throwable#getMessage} returns the validation error message.
 */
public class ValidationError extends Exception {
  /** Contains messages about unknown fields found while parsing. */
  public String[] unknownFieldMessages;

  ValidationError(String message, String[] unknownFields) {
    super(message);
    this.unknownFieldMessages = unknownFields;
  }
}
