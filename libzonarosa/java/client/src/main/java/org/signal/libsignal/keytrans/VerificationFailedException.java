//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.keytrans;

/** Key transparency data verification failed. */
public class VerificationFailedException extends KeyTransparencyException {
  public VerificationFailedException(String message) {
    super(message);
  }
}
