//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

public final class NicknameTooLongException extends BaseUsernameException {
  public NicknameTooLongException(String message) {
    super(message);
  }
}
