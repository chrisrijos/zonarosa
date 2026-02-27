//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

public final class DiscriminatorTooLargeException extends BadDiscriminatorException {
  public DiscriminatorTooLargeException(String message) {
    super(message);
  }
}
