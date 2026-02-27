//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.logging;

import io.zonarosa.libzonarosa.internal.Native;

public class ZonaRosaProtocolLoggerProvider {

  private static ZonaRosaProtocolLogger provider;

  /**
   * Enables logging from libzonarosa's native code.
   *
   * <p>Should only be called once; later calls will be ignored.
   *
   * @param maxLevel The most severe level that should be logged. Should be one of the constants
   *     from {@link ZonaRosaProtocolLogger}. In a normal release build, this is clamped to {@code
   *     INFO}.
   */
  public static void initializeLogging(int maxLevel) {
    if (maxLevel < ZonaRosaProtocolLogger.VERBOSE || maxLevel > ZonaRosaProtocolLogger.ASSERT) {
      throw new IllegalArgumentException("invalid log level");
    }
    Native.Logger_Initialize(maxLevel, Log.class);
  }

  public static ZonaRosaProtocolLogger getProvider() {
    return provider;
  }

  public static void setProvider(ZonaRosaProtocolLogger provider) {
    ZonaRosaProtocolLoggerProvider.provider = provider;
  }
}
