//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.message;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import javax.crypto.spec.SecretKeySpec;
import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.util.ByteUtil;

public class ZonaRosaMessage extends NativeHandleGuard.SimpleOwner
    implements CiphertextMessage, NativeHandleGuard.Owner {
  @Override
  protected void release(long nativeHandle) {
    Native.ZonaRosaMessage_Destroy(nativeHandle);
  }

  public ZonaRosaMessage(byte[] serialized)
      throws InvalidMessageException,
          InvalidVersionException,
          InvalidKeyException,
          LegacyMessageException {
    super(
        filterExceptions(
            InvalidMessageException.class,
            InvalidVersionException.class,
            InvalidKeyException.class,
            LegacyMessageException.class,
            () -> Native.ZonaRosaMessage_Deserialize(serialized)));
  }

  @CalledFromNative
  public ZonaRosaMessage(long nativeHandle) {
    super(nativeHandle);
  }

  public ECPublicKey getSenderRatchetKey() {
    return new ECPublicKey(
        filterExceptions(() -> guardedMapChecked(Native::ZonaRosaMessage_GetSenderRatchetKey)));
  }

  public int getMessageVersion() {
    return filterExceptions(() -> guardedMapChecked(Native::ZonaRosaMessage_GetMessageVersion));
  }

  public int getCounter() {
    return filterExceptions(() -> guardedMapChecked(Native::ZonaRosaMessage_GetCounter));
  }

  public byte[] getBody() {
    return filterExceptions(() -> guardedMapChecked(Native::ZonaRosaMessage_GetBody));
  }

  public byte[] getPqRatchet() {
    return filterExceptions(() -> guardedMapChecked(Native::ZonaRosaMessage_GetPqRatchet));
  }

  public void verifyMac(
      IdentityKey senderIdentityKey, IdentityKey receiverIdentityKey, SecretKeySpec macKey)
      throws InvalidMessageException, InvalidKeyException {
    try (NativeHandleGuard guard = new NativeHandleGuard(this);
        NativeHandleGuard senderIdentityGuard =
            new NativeHandleGuard(senderIdentityKey.getPublicKey());
        NativeHandleGuard receiverIdentityGuard =
            new NativeHandleGuard(receiverIdentityKey.getPublicKey()); ) {
      if (!filterExceptions(
          InvalidMessageException.class,
          InvalidKeyException.class,
          () ->
              Native.ZonaRosaMessage_VerifyMac(
                  guard.nativeHandle(),
                  senderIdentityGuard.nativeHandle(),
                  receiverIdentityGuard.nativeHandle(),
                  macKey.getEncoded()))) {
        throw new InvalidMessageException("Bad Mac!");
      }
    }
  }

  @Override
  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::ZonaRosaMessage_GetSerialized));
  }

  @Override
  public int getType() {
    return CiphertextMessage.WHISPER_TYPE;
  }

  public static boolean isLegacy(byte[] message) {
    return message != null
        && message.length >= 1
        && ByteUtil.highBitsToInt(message[0]) != CiphertextMessage.CURRENT_VERSION;
  }
}
