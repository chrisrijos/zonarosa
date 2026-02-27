//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.message;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.Optional;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public final class DecryptionErrorMessage extends NativeHandleGuard.SimpleOwner {

  @Override
  protected void release(long nativeHandle) {
    Native.DecryptionErrorMessage_Destroy(nativeHandle);
  }

  DecryptionErrorMessage(long nativeHandle) {
    super(nativeHandle);
  }

  public DecryptionErrorMessage(byte[] serialized)
      throws InvalidKeyException, InvalidMessageException {
    super(
        filterExceptions(
            InvalidKeyException.class,
            InvalidMessageException.class,
            () -> Native.DecryptionErrorMessage_Deserialize(serialized)));
  }

  public static DecryptionErrorMessage forOriginalMessage(
      byte[] originalBytes, int messageType, long timestamp, int originalSenderDeviceId) {
    return new DecryptionErrorMessage(
        filterExceptions(
            () ->
                Native.DecryptionErrorMessage_ForOriginalMessage(
                    originalBytes, messageType, timestamp, originalSenderDeviceId)));
  }

  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::DecryptionErrorMessage_GetSerialized));
  }

  public Optional<ECPublicKey> getRatchetKey() {
    long keyHandle = guardedMap(Native::DecryptionErrorMessage_GetRatchetKey);
    if (keyHandle == 0) {
      return Optional.empty();
    } else {
      return Optional.of(new ECPublicKey(keyHandle));
    }
  }

  public long getTimestamp() {
    return filterExceptions(() -> guardedMapChecked(Native::DecryptionErrorMessage_GetTimestamp));
  }

  public int getDeviceId() {
    return filterExceptions(() -> guardedMapChecked(Native::DecryptionErrorMessage_GetDeviceId));
  }

  /// For testing only
  public static DecryptionErrorMessage extractFromSerializedContent(byte[] serializedContentBytes)
      throws InvalidMessageException {
    return new DecryptionErrorMessage(
        filterExceptions(
            InvalidMessageException.class,
            () ->
                Native.DecryptionErrorMessage_ExtractFromSerializedContent(
                    serializedContentBytes)));
  }
}
