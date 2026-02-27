//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.hsmenclave;

public class EnclaveCommunicationFailureException extends Exception {
  public EnclaveCommunicationFailureException(String msg) {
    super(msg);
  }

  public EnclaveCommunicationFailureException(Throwable t) {
    super(t);
  }
}
