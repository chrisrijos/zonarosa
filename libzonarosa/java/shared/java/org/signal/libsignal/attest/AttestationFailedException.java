//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.attest;

/** An enclave failed attestation. */
public class AttestationFailedException extends Exception {
  public AttestationFailedException(String msg) {
    super(msg);
  }

  public AttestationFailedException(Throwable t) {
    super(t);
  }
}
