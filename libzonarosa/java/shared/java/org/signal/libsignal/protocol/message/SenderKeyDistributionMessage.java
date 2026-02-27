//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.message;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.UUID;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public class SenderKeyDistributionMessage extends NativeHandleGuard.SimpleOwner {

  @Override
  protected void release(long nativeHandle) {
    Native.SenderKeyDistributionMessage_Destroy(nativeHandle);
  }

  public SenderKeyDistributionMessage(long nativeHandle) {
    super(nativeHandle);
  }

  public SenderKeyDistributionMessage(byte[] serialized)
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
            () -> Native.SenderKeyDistributionMessage_Deserialize(serialized)));
  }

  public byte[] serialize() {
    return filterExceptions(
        () -> guardedMapChecked(Native::SenderKeyDistributionMessage_GetSerialized));
  }

  public UUID getDistributionId() {
    return filterExceptions(
        () -> guardedMapChecked(Native::SenderKeyDistributionMessage_GetDistributionId));
  }

  public int getIteration() {
    return filterExceptions(
        () -> guardedMapChecked(Native::SenderKeyDistributionMessage_GetIteration));
  }

  public byte[] getChainKey() {
    return filterExceptions(
        () -> guardedMapChecked(Native::SenderKeyDistributionMessage_GetChainKey));
  }

  public ECPublicKey getSignatureKey() {
    return new ECPublicKey(
        filterExceptions(
            () -> guardedMapChecked(Native::SenderKeyDistributionMessage_GetSignatureKey)));
  }

  public int getChainId() {
    return filterExceptions(
        () -> guardedMapChecked(Native::SenderKeyDistributionMessage_GetChainId));
  }
}
