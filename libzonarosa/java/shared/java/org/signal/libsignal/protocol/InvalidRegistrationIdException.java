//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

public class InvalidRegistrationIdException extends Exception {

  private final ZonaRosaProtocolAddress address;

  public InvalidRegistrationIdException(ZonaRosaProtocolAddress address, String message) {
    super(message);
    this.address = address;
  }

  public ZonaRosaProtocolAddress getAddress() {
    return address;
  }
}
