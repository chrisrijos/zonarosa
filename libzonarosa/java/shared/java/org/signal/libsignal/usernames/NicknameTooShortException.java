//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

public final class NicknameTooShortException extends BaseUsernameException {
  public NicknameTooShortException(String message) {
    super(message);
  }
}
