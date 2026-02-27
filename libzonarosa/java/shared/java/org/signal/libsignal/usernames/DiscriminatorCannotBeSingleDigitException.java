//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

public final class DiscriminatorCannotBeSingleDigitException extends BadDiscriminatorException {
  public DiscriminatorCannotBeSingleDigitException(String message) {
    super(message);
  }
}
