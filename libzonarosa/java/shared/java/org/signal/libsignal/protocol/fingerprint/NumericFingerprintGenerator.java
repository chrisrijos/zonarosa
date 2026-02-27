//
// Copyright 2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.fingerprint;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.IdentityKey;

public class NumericFingerprintGenerator implements FingerprintGenerator {
  private final int iterations;

  /**
   * Construct a fingerprint generator for 60 digit numerics.
   *
   * @param iterations The number of internal iterations to perform in the process of generating a
   *     fingerprint. This needs to be constant, and synchronized across all clients.
   *     <p>The higher the iteration count, the higher the security level:
   *     <ul>
   *       <li>1024 ~ 109.7 bits
   *       <li>1400 &gt; 110 bits
   *       <li>5200 &gt; 112 bits
   *     </ul>
   */
  public NumericFingerprintGenerator(int iterations) {
    this.iterations = iterations;
  }

  /**
   * Generate a scannable and displayable fingerprint.
   *
   * @param version The version of fingerprint you are generating.
   * @param localStableIdentifier The client's "stable" identifier.
   * @param localIdentityKey The client's identity key.
   * @param remoteStableIdentifier The remote party's "stable" identifier.
   * @param remoteIdentityKey The remote party's identity key.
   * @return A unique fingerprint for this conversation.
   */
  @Override
  public Fingerprint createFor(
      int version,
      byte[] localStableIdentifier,
      final IdentityKey localIdentityKey,
      byte[] remoteStableIdentifier,
      final IdentityKey remoteIdentityKey) {

    try (var localIdentityKeyGuard = localIdentityKey.getPublicKey().guard();
        var remoteIdentityKeyGuard = remoteIdentityKey.getPublicKey().guard(); ) {
      return filterExceptions(
          () -> {
            long handle =
                Native.NumericFingerprintGenerator_New(
                    this.iterations,
                    version,
                    localStableIdentifier,
                    localIdentityKeyGuard.nativeHandle(),
                    remoteStableIdentifier,
                    remoteIdentityKeyGuard.nativeHandle());

            DisplayableFingerprint displayableFingerprint =
                new DisplayableFingerprint(
                    Native.NumericFingerprintGenerator_GetDisplayString(handle));

            ScannableFingerprint scannableFingerprint =
                new ScannableFingerprint(
                    Native.NumericFingerprintGenerator_GetScannableEncoding(handle));

            Native.NumericFingerprintGenerator_Destroy(handle);

            return new Fingerprint(displayableFingerprint, scannableFingerprint);
          });
    }
  }
}
