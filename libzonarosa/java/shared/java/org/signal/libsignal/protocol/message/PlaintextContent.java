//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.message;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;

public final class PlaintextContent extends NativeHandleGuard.SimpleOwner
    implements CiphertextMessage, NativeHandleGuard.Owner {

  @Override
  protected void release(long nativeHandle) {
    Native.PlaintextContent_Destroy(nativeHandle);
  }

  @CalledFromNative
  @SuppressWarnings("unused")
  private PlaintextContent(long nativeHandle) {
    super(nativeHandle);
  }

  public PlaintextContent(DecryptionErrorMessage message) {
    super(message.guardedMap(Native::PlaintextContent_FromDecryptionErrorMessage));
  }

  public PlaintextContent(byte[] serialized)
      throws InvalidMessageException, InvalidVersionException {
    super(
        filterExceptions(
            InvalidMessageException.class,
            InvalidVersionException.class,
            () -> Native.PlaintextContent_Deserialize(serialized)));
  }

  @Override
  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::PlaintextContent_GetSerialized));
  }

  @Override
  public int getType() {
    return CiphertextMessage.PLAINTEXT_CONTENT_TYPE;
  }

  public byte[] getBody() {
    return filterExceptions(() -> guardedMapChecked(Native::PlaintextContent_GetBody));
  }
}
