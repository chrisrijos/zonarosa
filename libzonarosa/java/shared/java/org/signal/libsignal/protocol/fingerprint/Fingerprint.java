//
// Copyright 2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.fingerprint;

public class Fingerprint {

  private final DisplayableFingerprint displayableFingerprint;
  private final ScannableFingerprint scannableFingerprint;

  public Fingerprint(
      DisplayableFingerprint displayableFingerprint, ScannableFingerprint scannableFingerprint) {
    this.displayableFingerprint = displayableFingerprint;
    this.scannableFingerprint = scannableFingerprint;
  }

  /**
   * @return A text fingerprint that can be displayed and compared remotely.
   */
  public DisplayableFingerprint getDisplayableFingerprint() {
    return displayableFingerprint;
  }

  /**
   * @return A scannable fingerprint that can be scanned and compared locally.
   */
  public ScannableFingerprint getScannableFingerprint() {
    return scannableFingerprint;
  }
}
