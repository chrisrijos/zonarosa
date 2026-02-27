//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.usernames;

import io.zonarosa.libzonarosa.net.LookUpUsernameLinkFailure;

public class UsernameLinkInvalidEntropyDataLength extends BaseUsernameException
    implements LookUpUsernameLinkFailure {
  public UsernameLinkInvalidEntropyDataLength(final String message) {
    super(message);
  }
}
