//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public class SignedPreKeyRecord extends NativeHandleGuard.SimpleOwner {
  @Override
  protected void release(long nativeHandle) {
    Native.SignedPreKeyRecord_Destroy(nativeHandle);
  }

  public SignedPreKeyRecord(int id, long timestamp, ECKeyPair keyPair, byte[] signature) {
    super(
        keyPair
            .getPublicKey()
            .guardedMap(
                (publicKeyHandle) ->
                    keyPair
                        .getPrivateKey()
                        .guardedMap(
                            (privateKeyHandle) ->
                                Native.SignedPreKeyRecord_New(
                                    id, timestamp, publicKeyHandle, privateKeyHandle, signature))));
  }

  // FIXME: This shouldn't be considered a "message".
  public SignedPreKeyRecord(byte[] serialized) throws InvalidMessageException {
    super(
        filterExceptions(
            InvalidMessageException.class,
            () -> Native.SignedPreKeyRecord_Deserialize(serialized)));
  }

  public SignedPreKeyRecord(long nativeHandle) {
    super(nativeHandle);
  }

  public int getId() {
    return filterExceptions(() -> guardedMapChecked(Native::SignedPreKeyRecord_GetId));
  }

  public long getTimestamp() {
    return filterExceptions(() -> guardedMapChecked(Native::SignedPreKeyRecord_GetTimestamp));
  }

  public ECKeyPair getKeyPair() throws InvalidKeyException {
    return filterExceptions(
        InvalidKeyException.class,
        () ->
            guardedMapChecked(
                (nativeHandle) -> {
                  ECPublicKey publicKey =
                      new ECPublicKey(Native.SignedPreKeyRecord_GetPublicKey(nativeHandle));
                  ECPrivateKey privateKey =
                      new ECPrivateKey(Native.SignedPreKeyRecord_GetPrivateKey(nativeHandle));
                  return new ECKeyPair(publicKey, privateKey);
                }));
  }

  public byte[] getSignature() {
    return filterExceptions(() -> guardedMapChecked(Native::SignedPreKeyRecord_GetSignature));
  }

  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::SignedPreKeyRecord_GetSerialized));
  }
}
