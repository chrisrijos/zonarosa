//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.util;

import java.time.LocalTime;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLogger;

public class StderrLogger implements ZonaRosaProtocolLogger {
  @Override
  public void log(int priority, String tag, String message) {
    String prefix;
    switch (priority) {
      case ZonaRosaProtocolLogger.VERBOSE:
        prefix = "V ";
        break;
      case ZonaRosaProtocolLogger.DEBUG:
        prefix = "D ";
        break;
      case ZonaRosaProtocolLogger.INFO:
        prefix = "I ";
        break;
      case ZonaRosaProtocolLogger.WARN:
        prefix = "W ";
        break;
      case ZonaRosaProtocolLogger.ERROR:
        prefix = "E ";
        break;
      case ZonaRosaProtocolLogger.ASSERT:
        prefix = "A ";
        break;
      default:
        prefix = "";
        break;
    }
    System.err.println("[" + LocalTime.now() + " " + tag + "] " + prefix + message);
  }
}
