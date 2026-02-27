/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public class HostnameUtil {

  private static final Logger log = LoggerFactory.getLogger(HostnameUtil.class);

  public static String getLocalHostname() {
    try {
      return InetAddress.getLocalHost().getHostName().toLowerCase(Locale.US);
    } catch (final UnknownHostException e) {
      log.warn("Failed to get hostname", e);
      return "unknown";
    }
  }
}
