//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.attest;

/** Attestation data was malformed. */
public class AttestationDataException extends Exception {
  public AttestationDataException(String msg) {
    super(msg);
  }

  public AttestationDataException(Throwable t) {
    super(t);
  }
}
