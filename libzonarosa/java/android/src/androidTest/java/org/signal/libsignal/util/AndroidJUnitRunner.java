//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.util;

import android.os.Bundle;
import io.zonarosa.libzonarosa.protocol.logging.AndroidZonaRosaProtocolLogger;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLogger;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLoggerProvider;

/** Custom setup for our JUnit tests, when run as instrumentation tests. */
public class AndroidJUnitRunner extends androidx.test.runner.AndroidJUnitRunner {
  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    // Make sure libzonarosa logs get caught correctly.
    ZonaRosaProtocolLoggerProvider.setProvider(new AndroidZonaRosaProtocolLogger());
    ZonaRosaProtocolLoggerProvider.initializeLogging(ZonaRosaProtocolLogger.VERBOSE);

    // Propagate any "environment variables" the test might need into System properties.
    String testEnvironment = bundle.getString(TestEnvironment.PROPERTY_NAMESPACE);
    if (testEnvironment != null) {
      for (String joinedProp : testEnvironment.split(",")) {
        String[] splitProp = joinedProp.split("=", 2);
        if (splitProp.length != 2) {
          continue;
        }
        System.setProperty(TestEnvironment.PROPERTY_NAMESPACE + "." + splitProp[0], splitProp[1]);
      }
    }
  }
}
