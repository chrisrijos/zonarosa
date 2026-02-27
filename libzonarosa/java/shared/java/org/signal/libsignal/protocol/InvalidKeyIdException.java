//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

public class InvalidKeyIdException extends Exception {
  public InvalidKeyIdException(String detailMessage) {
    super(detailMessage);
  }

  public InvalidKeyIdException(Throwable throwable) {
    super(throwable);
  }
}
