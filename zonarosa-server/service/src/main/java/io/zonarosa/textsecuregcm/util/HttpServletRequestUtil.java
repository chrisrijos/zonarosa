/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import jakarta.servlet.http.HttpServletRequest;

public class HttpServletRequestUtil {

  /**
   * Returns the remote address of the request, removing bracket ("[…]") host notation from IPv6 addresses present in
   * some implementations, notably {@link org.eclipse.jetty.server.HttpChannel}.
   */
  public static String getRemoteAddress(final HttpServletRequest request) {
    final String remoteAddr = request.getRemoteAddr();

    if (remoteAddr.startsWith("[")) {
      return remoteAddr.substring(1, remoteAddr.length() - 1);
    }

    return remoteAddr;
  }
}
