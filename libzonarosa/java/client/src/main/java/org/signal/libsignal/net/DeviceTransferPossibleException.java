//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

public class DeviceTransferPossibleException extends RegistrationException {
  @CalledFromNative
  private DeviceTransferPossibleException(String message) {
    super(message);
  }
}
