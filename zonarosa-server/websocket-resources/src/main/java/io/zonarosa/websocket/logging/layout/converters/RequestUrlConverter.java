/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.logging.layout.converters;

import io.zonarosa.websocket.logging.WebsocketEvent;

public class RequestUrlConverter extends WebSocketEventConverter {
  @Override
  public String convert(WebsocketEvent event) {
    return
        event.getMethod()                  +
        WebSocketEventConverter.SPACE_CHAR +
        event.getPath()                    +
        WebSocketEventConverter.SPACE_CHAR +
        event.getProtocol();
  }
}
