//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

public class NoSessionException extends Exception {
  private final ZonaRosaProtocolAddress address;

  public NoSessionException(String message) {
    this(null, message);
  }

  public NoSessionException(ZonaRosaProtocolAddress address, String message) {
    super(message);
    this.address = address;
  }

  public ZonaRosaProtocolAddress getAddress() {
    return address;
  }
}
