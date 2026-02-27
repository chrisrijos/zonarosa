//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.util;

import org.junit.rules.ExternalResource;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLogger;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLoggerProvider;

public class TestLogger extends ExternalResource {
  private static boolean loggerInitialized = false;

  private static synchronized void ensureLoggerInitialized() {
    if (loggerInitialized) {
      return;
    }
    loggerInitialized = true;

    // AndroidJUnitRunner sets up its own logging, so if that's available, we're done.
    try {
      Class.forName("io.zonarosa.libzonarosa.util.AndroidJUnitRunner");
      return;
    } catch (ClassNotFoundException e) {
      // Okay, we're not running as an Android instrumented test.
    }

    ZonaRosaProtocolLoggerProvider.initializeLogging(ZonaRosaProtocolLogger.VERBOSE);
    ZonaRosaProtocolLoggerProvider.setProvider(new StderrLogger());
  }

  @Override
  protected void before() throws Throwable {
    ensureLoggerInitialized();
  }
}
