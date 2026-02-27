/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server;

public class WhisperServerVersion {

  private static final String VERSION = "${project.version}";

  public static String getServerVersion() {
    return VERSION;
  }
}
