//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.hsmenclave;

public class TrustedCodeMismatchException extends Exception {
  public TrustedCodeMismatchException(String msg) {
    super(msg);
  }

  public TrustedCodeMismatchException(Throwable t) {
    super(t);
  }
}
