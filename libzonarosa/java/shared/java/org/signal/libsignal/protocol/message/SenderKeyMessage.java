//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.message;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.UUID;
import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public class SenderKeyMessage extends NativeHandleGuard.SimpleOwner
    implements CiphertextMessage, NativeHandleGuard.Owner {

  @Override
  protected void release(long nativeHandle) {
    Native.SenderKeyMessage_Destroy(nativeHandle);
  }

  @CalledFromNative
  public SenderKeyMessage(long nativeHandle) {
    super(nativeHandle);
  }

  public SenderKeyMessage(byte[] serialized)
      throws InvalidMessageException, InvalidVersionException, LegacyMessageException {
    super(
        filterExceptions(
            InvalidMessageException.class,
            InvalidVersionException.class,
            LegacyMessageException.class,
            () -> Native.SenderKeyMessage_Deserialize(serialized)));
  }

  public UUID getDistributionId() {
    return filterExceptions(() -> guardedMapChecked(Native::SenderKeyMessage_GetDistributionId));
  }

  public int getChainId() {
    return filterExceptions(() -> guardedMapChecked(Native::SenderKeyMessage_GetChainId));
  }

  public int getIteration() {
    return filterExceptions(() -> guardedMapChecked(Native::SenderKeyMessage_GetIteration));
  }

  public byte[] getCipherText() {
    return filterExceptions(() -> guardedMapChecked(Native::SenderKeyMessage_GetCipherText));
  }

  public void verifySignature(ECPublicKey signatureKey) throws InvalidMessageException {
    try (NativeHandleGuard guard = new NativeHandleGuard(this);
        NativeHandleGuard keyGuard = new NativeHandleGuard(signatureKey); ) {
      if (!filterExceptions(
          InvalidMessageException.class,
          () ->
              Native.SenderKeyMessage_VerifySignature(
                  guard.nativeHandle(), keyGuard.nativeHandle()))) {
        throw new InvalidMessageException("Invalid signature!");
      }
    }
  }

  @Override
  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::SenderKeyMessage_GetSerialized));
  }

  @Override
  public int getType() {
    return CiphertextMessage.SENDERKEY_TYPE;
  }
}
