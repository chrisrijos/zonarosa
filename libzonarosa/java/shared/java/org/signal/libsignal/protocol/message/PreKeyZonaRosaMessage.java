//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.message;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.Optional;
import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public class PreKeyZonaRosaMessage extends NativeHandleGuard.SimpleOwner
    implements CiphertextMessage, NativeHandleGuard.Owner {

  @Override
  protected void release(long nativeHandle) {
    Native.PreKeyZonaRosaMessage_Destroy(nativeHandle);
  }

  public PreKeyZonaRosaMessage(byte[] serialized)
      throws InvalidMessageException,
          InvalidVersionException,
          LegacyMessageException,
          InvalidKeyException {
    super(
        filterExceptions(
            InvalidMessageException.class,
            InvalidVersionException.class,
            LegacyMessageException.class,
            InvalidKeyException.class,
            () -> Native.PreKeyZonaRosaMessage_Deserialize(serialized)));
  }

  @CalledFromNative
  public PreKeyZonaRosaMessage(long nativeHandle) {
    super(nativeHandle);
  }

  public int getMessageVersion() {
    return filterExceptions(() -> guardedMapChecked(Native::PreKeyZonaRosaMessage_GetVersion));
  }

  public IdentityKey getIdentityKey() {
    return new IdentityKey(
        filterExceptions(() -> guardedMapChecked(Native::PreKeyZonaRosaMessage_GetIdentityKey)));
  }

  public int getRegistrationId() {
    return filterExceptions(() -> guardedMapChecked(Native::PreKeyZonaRosaMessage_GetRegistrationId));
  }

  public Optional<Integer> getPreKeyId() {
    int pre_key =
        filterExceptions(() -> guardedMapChecked(Native::PreKeyZonaRosaMessage_GetPreKeyId));
    if (pre_key < 0) {
      return Optional.empty();
    } else {
      return Optional.of(pre_key);
    }
  }

  public int getSignedPreKeyId() {
    return filterExceptions(() -> guardedMapChecked(Native::PreKeyZonaRosaMessage_GetSignedPreKeyId));
  }

  public ECPublicKey getBaseKey() {
    return new ECPublicKey(guardedMap(Native::PreKeyZonaRosaMessage_GetBaseKey));
  }

  public ZonaRosaMessage getWhisperMessage() {
    return new ZonaRosaMessage(guardedMap(Native::PreKeyZonaRosaMessage_GetZonaRosaMessage));
  }

  @Override
  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::PreKeyZonaRosaMessage_GetSerialized));
  }

  @Override
  public int getType() {
    return CiphertextMessage.PREKEY_TYPE;
  }
}
