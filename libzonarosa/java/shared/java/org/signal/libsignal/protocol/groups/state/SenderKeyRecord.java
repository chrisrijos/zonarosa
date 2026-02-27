//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.groups.state;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;

/**
 * A durable representation of a set of SenderKeyStates for a specific (senderName, deviceId,
 * distributionId) tuple.
 *
 * @author Moxie Marlinspike
 */
public class SenderKeyRecord extends NativeHandleGuard.SimpleOwner {
  @Override
  protected void release(long nativeHandle) {
    Native.SenderKeyRecord_Destroy(nativeHandle);
  }

  @CalledFromNative
  public SenderKeyRecord(long nativeHandle) {
    super(nativeHandle);
  }

  // FIXME: This shouldn't be considered a "message".
  public SenderKeyRecord(byte[] serialized) throws InvalidMessageException {
    super(
        filterExceptions(
            InvalidMessageException.class, () -> Native.SenderKeyRecord_Deserialize(serialized)));
  }

  public byte[] serialize() {
    return filterExceptions(() -> guardedMapChecked(Native::SenderKeyRecord_GetSerialized));
  }
}
