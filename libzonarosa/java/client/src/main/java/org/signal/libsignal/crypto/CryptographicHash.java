//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.crypto;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;

public class CryptographicHash extends NativeHandleGuard.SimpleOwner {
  public CryptographicHash(String algo) {
    super(filterExceptions(() -> Native.CryptographicHash_New(algo)));
  }

  @Override
  protected void release(long nativeHandle) {
    Native.CryptographicHash_Destroy(nativeHandle);
  }

  public void update(byte[] input, int offset, int len) {
    guardedRun(
        (nativeHandle) ->
            Native.CryptographicHash_UpdateWithOffset(nativeHandle, input, offset, len));
  }

  public void update(byte[] input) {
    guardedRun((nativeHandle) -> Native.CryptographicHash_Update(nativeHandle, input));
  }

  public byte[] finish() {
    return guardedMap(Native::CryptographicHash_Finalize);
  }
}
