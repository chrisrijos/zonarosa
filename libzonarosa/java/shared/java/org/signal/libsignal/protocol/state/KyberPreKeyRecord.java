//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair;

public class KyberPreKeyRecord extends NativeHandleGuard.SimpleOwner {
  @Override
  protected void release(long nativeHandle) {
    Native.KyberPreKeyRecord_Destroy(nativeHandle);
  }

  public KyberPreKeyRecord(int id, long timestamp, KEMKeyPair keyPair, byte[] signature) {
    super(
        keyPair.guardedMap(
            (keyPairHandle) ->
                Native.KyberPreKeyRecord_New(id, timestamp, keyPairHandle, signature)));
  }

  // FIXME: This shouldn't be considered a "message".
  public KyberPreKeyRecord(byte[] serialized) throws InvalidMessageException {
    super(
        filterExceptions(
            InvalidMessageException.class, () -> Native.KyberPreKeyRecord_Deserialize(serialized)));
  }

  public KyberPreKeyRecord(long nativeHandle) {
    super(nativeHandle);
  }

  public int getId() {
    return filterExceptions(() -> guardedMapChecked(Native::KyberPreKeyRecord_GetId));
  }

  public long getTimestamp() {
    return filterExceptions(() -> guardedMapChecked(Native::KyberPreKeyRecord_GetTimestamp));
  }

  public KEMKeyPair getKeyPair() throws InvalidKeyException {
    return new KEMKeyPair(
        filterExceptions(
            InvalidKeyException.class,
            () -> guardedMapChecked(Native::KyberPreKeyRecord_GetKeyPair)));
  }

  public byte[] getSignature() {
    return filterExceptions(() -> guardedMapChecked(Native::KyberPreKeyRecord_GetSignature));
  }

  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::KyberPreKeyRecord_GetSerialized));
  }
}
