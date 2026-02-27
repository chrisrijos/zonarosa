//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.util;

public class TestEnvironment {
  public static final String PROPERTY_NAMESPACE = "io.zonarosa.libzonarosa.test.environment";

  /**
   * Looks in the environment first, then in System properties, namespaced by {@link
   * #PROPERTY_NAMESPACE}.
   *
   * <p>The Android runner manually propagates environment variable into the namespaced system
   * properties.
   */
  public static String get(final String name) {
    final String result = System.getenv(name);
    if (result != null) {
      return result;
    }
    return System.getProperty(PROPERTY_NAMESPACE + "." + name);
  }
}
