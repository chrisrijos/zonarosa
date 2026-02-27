/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.logging.layout.converters;

import io.zonarosa.websocket.logging.WebsocketEvent;

import ch.qos.logback.core.CoreConstants;

public class LineSeparatorConverter extends WebSocketEventConverter {
  public LineSeparatorConverter() {
  }

  public String convert(WebsocketEvent event) {
    return CoreConstants.LINE_SEPARATOR;
  }
}
