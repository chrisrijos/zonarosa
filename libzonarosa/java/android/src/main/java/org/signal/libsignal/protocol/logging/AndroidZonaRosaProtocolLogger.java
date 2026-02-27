//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.logging;

import android.util.Log;
import android.util.SparseIntArray;

public class AndroidZonaRosaProtocolLogger implements ZonaRosaProtocolLogger {

  private static final SparseIntArray PRIORITY_MAP =
      new SparseIntArray(5) {
        {
          put(ZonaRosaProtocolLogger.INFO, Log.INFO);
          put(ZonaRosaProtocolLogger.ASSERT, Log.ASSERT);
          put(ZonaRosaProtocolLogger.DEBUG, Log.DEBUG);
          put(ZonaRosaProtocolLogger.VERBOSE, Log.VERBOSE);
          put(ZonaRosaProtocolLogger.WARN, Log.WARN);
        }
      };

  @Override
  public void log(int priority, String tag, String message) {
    int androidPriority = PRIORITY_MAP.get(priority, Log.WARN);
    Log.println(androidPriority, tag, message);
  }
}
