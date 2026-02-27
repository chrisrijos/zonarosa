/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util.logging;

import org.glassfish.jersey.server.ExtendedUriInfo;

public class UriInfoUtil {

  public static String getPathTemplate(final ExtendedUriInfo uriInfo) {
      final StringBuilder pathBuilder = new StringBuilder();

      for (int i = uriInfo.getMatchedTemplates().size() - 1; i >= 0; i--) {
          pathBuilder.append(uriInfo.getMatchedTemplates().get(i).getTemplate());
      }

      return pathBuilder.toString();
  }
}
