/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.net.URL;

final class URLSerializationConverter extends StdConverter<URL, String> {

  @Override
  public String convert(final URL value) {
    return value.toString();
  }
}
