//
// Copyright 2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.fingerprint;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;

public class ScannableFingerprint {
  private final byte[] encodedFingerprint;

  ScannableFingerprint(byte[] encodedFingerprint) {
    this.encodedFingerprint = encodedFingerprint;
  }

  /**
   * @return A byte string to be displayed in a QR code.
   */
  public byte[] getSerialized() {
    return this.encodedFingerprint;
  }

  /**
   * Native.ScannableFingerprint_Compare a scanned QR code with what we expect.
   *
   * @param scannedFingerprintData The scanned data
   * @return True if matching, otherwise false.
   * @throws FingerprintVersionMismatchException if the scanned fingerprint is the wrong version.
   */
  public boolean compareTo(byte[] scannedFingerprintData)
      throws FingerprintVersionMismatchException, FingerprintParsingException {
    return filterExceptions(
        FingerprintVersionMismatchException.class,
        FingerprintParsingException.class,
        () -> Native.ScannableFingerprint_Compare(this.encodedFingerprint, scannedFingerprintData));
  }
}
