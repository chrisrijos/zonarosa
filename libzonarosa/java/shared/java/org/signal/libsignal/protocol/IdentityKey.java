//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.util.Hex;

/**
 * A class for representing an identity key.
 *
 * @author Moxie Marlinspike
 */
public class IdentityKey {

  private final ECPublicKey publicKey;

  public IdentityKey(ECPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  public IdentityKey(byte[] bytes, int offset) throws InvalidKeyException {
    this.publicKey = new ECPublicKey(bytes, offset);
  }

  public IdentityKey(byte[] bytes) throws InvalidKeyException {
    this.publicKey = new ECPublicKey(bytes, 0);
  }

  @CalledFromNative
  public IdentityKey(long nativeHandle) {
    this.publicKey = new ECPublicKey(nativeHandle);
  }

  public ECPublicKey getPublicKey() {
    return publicKey;
  }

  @CalledFromNative
  public byte[] serialize() {
    return publicKey.serialize();
  }

  public String getFingerprint() {
    return Hex.toString(publicKey.serialize());
  }

  public boolean verifyAlternateIdentity(IdentityKey other, byte[] signature) {
    try (NativeHandleGuard guard = new NativeHandleGuard(this.publicKey);
        NativeHandleGuard otherGuard = new NativeHandleGuard(other.publicKey); ) {
      return filterExceptions(
          () ->
              Native.IdentityKey_VerifyAlternateIdentity(
                  guard.nativeHandle(), otherGuard.nativeHandle(), signature));
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) return false;
    if (!(other instanceof IdentityKey)) return false;

    return publicKey.equals(((IdentityKey) other).getPublicKey());
  }

  @Override
  public int hashCode() {
    return publicKey.hashCode();
  }
}
