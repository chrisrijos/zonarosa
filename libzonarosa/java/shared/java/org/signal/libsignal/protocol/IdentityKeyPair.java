//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

/**
 * Holder for public and private identity key pair.
 *
 * @author Moxie Marlinspike
 */
public class IdentityKeyPair {
  private final IdentityKey publicKey;
  private final ECPrivateKey privateKey;

  public IdentityKeyPair(IdentityKey publicKey, ECPrivateKey privateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  public IdentityKeyPair(byte[] serialized) throws InvalidKeyException {
    try {
      var pair = Native.IdentityKeyPair_Deserialize(serialized);
      this.publicKey = new IdentityKey(pair.getFirst());
      this.privateKey = new ECPrivateKey(pair.getSecond());
    } catch (Exception e) {
      throw new InvalidKeyException(e);
    }
  }

  public static IdentityKeyPair generate() {
    ECPrivateKey privateKey = ECPrivateKey.generate();
    ECPublicKey publicKey = privateKey.publicKey();
    return new IdentityKeyPair(new IdentityKey(publicKey), privateKey);
  }

  public IdentityKey getPublicKey() {
    return publicKey;
  }

  public ECPrivateKey getPrivateKey() {
    return privateKey;
  }

  @CalledFromNative
  public byte[] serialize() {
    try (NativeHandleGuard publicKey = new NativeHandleGuard(this.publicKey.getPublicKey());
        NativeHandleGuard privateKey = new NativeHandleGuard(this.privateKey); ) {
      return Native.IdentityKeyPair_Serialize(publicKey.nativeHandle(), privateKey.nativeHandle());
    }
  }

  public byte[] signAlternateIdentity(IdentityKey other) {
    try (NativeHandleGuard publicKey = new NativeHandleGuard(this.publicKey.getPublicKey());
        NativeHandleGuard privateKey = new NativeHandleGuard(this.privateKey);
        NativeHandleGuard otherPublic = new NativeHandleGuard(other.getPublicKey()); ) {
      return filterExceptions(
          () ->
              Native.IdentityKeyPair_SignAlternateIdentity(
                  publicKey.nativeHandle(), privateKey.nativeHandle(), otherPublic.nativeHandle()));
    }
  }
}
