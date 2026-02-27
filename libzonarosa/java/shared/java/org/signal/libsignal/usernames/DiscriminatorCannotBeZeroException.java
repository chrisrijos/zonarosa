//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

public final class DiscriminatorCannotBeZeroException extends BadDiscriminatorException {
  public DiscriminatorCannotBeZeroException(String message) {
    super(message);
  }
}
