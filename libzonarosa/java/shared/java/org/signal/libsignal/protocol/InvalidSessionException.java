//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

public class InvalidSessionException extends IllegalStateException {
  public InvalidSessionException(String detailMessage) {
    super(detailMessage);
  }

  public InvalidSessionException(String detailMessage, Throwable cause) {
    super(detailMessage, cause);
  }
}
