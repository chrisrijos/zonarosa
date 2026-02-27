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

public class PreKeyRecord extends NativeHandleGuard.SimpleOwner {
  @Override
  protected void release(long nativeHandle) {
    Native.PreKeyRecord_Destroy(nativeHandle);
  }

  public PreKeyRecord(long nativeHandle) {
    super(nativeHandle);
  }

  public PreKeyRecord(int id, ECKeyPair keyPair) {
    super(
        keyPair
            .getPublicKey()
            .guardedMap(
                (publicKeyHandle) ->
                    keyPair
                        .getPrivateKey()
                        .guardedMap(
                            (privateKeyHandle) ->
                                Native.PreKeyRecord_New(id, publicKeyHandle, privateKeyHandle))));
  }

  // FIXME: This shouldn't be considered a "message".
  public PreKeyRecord(byte[] serialized) throws InvalidMessageException {
    super(
        filterExceptions(
            InvalidMessageException.class, () -> Native.PreKeyRecord_Deserialize(serialized)));
  }

  public int getId() {
    return filterExceptions(() -> guardedMapChecked(Native::PreKeyRecord_GetId));
  }

  public ECKeyPair getKeyPair() throws InvalidKeyException {
    try (NativeHandleGuard guard = new NativeHandleGuard(this)) {
      return filterExceptions(
          InvalidKeyException.class,
          () -> {
            ECPublicKey publicKey =
                new ECPublicKey(Native.PreKeyRecord_GetPublicKey(guard.nativeHandle()));
            ECPrivateKey privateKey =
                new ECPrivateKey(Native.PreKeyRecord_GetPrivateKey(guard.nativeHandle()));
            return new ECKeyPair(publicKey, privateKey);
          });
    }
  }

  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::PreKeyRecord_GetSerialized));
  }
}
