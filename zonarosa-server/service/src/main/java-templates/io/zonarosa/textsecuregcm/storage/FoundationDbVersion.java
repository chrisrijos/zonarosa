/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

public class FoundationDbVersion {

  private static final String VERSION = "${foundationdb.version}";
  private static final int API_VERSION = ${foundationdb.api-version};

  public static String getFoundationDbVersion() {
    return VERSION;
  }

  public static int getFoundationDbApiVersion() {
    return API_VERSION;
  }
}
