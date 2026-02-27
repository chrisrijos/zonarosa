//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.crypto;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;

public class CryptographicMac extends NativeHandleGuard.SimpleOwner {
  public CryptographicMac(String algo, byte[] key) {
    super(filterExceptions(() -> Native.CryptographicMac_New(algo, key)));
  }

  @Override
  protected void release(long nativeHandle) {
    Native.CryptographicMac_Destroy(nativeHandle);
  }

  public void update(byte[] input, int offset, int len) {
    guardedRun(
        (nativeHandle) ->
            Native.CryptographicMac_UpdateWithOffset(nativeHandle, input, offset, len));
  }

  public void update(byte[] input) {
    guardedRun((nativeHandle) -> Native.CryptographicMac_Update(nativeHandle, input));
  }

  public byte[] finish() {
    return guardedMap(Native::CryptographicMac_Finalize);
  }
}
