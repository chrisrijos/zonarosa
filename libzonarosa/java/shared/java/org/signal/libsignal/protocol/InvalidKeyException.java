//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

public class InvalidKeyException extends Exception {

  public InvalidKeyException() {}

  public InvalidKeyException(String detailMessage) {
    super(detailMessage);
  }

  public InvalidKeyException(Throwable throwable) {
    super(throwable);
  }

  public InvalidKeyException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }
}
