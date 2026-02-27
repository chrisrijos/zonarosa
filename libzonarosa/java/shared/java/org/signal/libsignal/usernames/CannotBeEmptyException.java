//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

public final class CannotBeEmptyException extends BaseUsernameException {
  public CannotBeEmptyException(String message) {
    super(message);
  }
}
