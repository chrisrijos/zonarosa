//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.internal;

import java.util.Map;

public class BridgedStringMap extends NativeHandleGuard.SimpleOwner {
  public BridgedStringMap(Map<String, String> map) {
    super(Native.BridgedStringMap_new(map.size()));
    guardedRun(
        handle -> {
          map.forEach((k, v) -> Native.BridgedStringMap_insert(handle, k, v));
        });
  }

  protected void release(long nativeHandle) {
    Native.BridgedStringMap_Destroy(nativeHandle);
  }

  String dump() {
    return guardedMap(NativeTesting::TESTING_BridgedStringMap_dump_to_json);
  }
}
