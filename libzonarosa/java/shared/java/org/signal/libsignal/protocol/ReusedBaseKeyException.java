//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

public class ReusedBaseKeyException extends InvalidMessageException {

  public ReusedBaseKeyException() {}

  public ReusedBaseKeyException(String detailMessage) {
    super(detailMessage);
  }

  public ReusedBaseKeyException(Throwable throwable) {
    super(throwable);
  }

  public ReusedBaseKeyException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }
}
