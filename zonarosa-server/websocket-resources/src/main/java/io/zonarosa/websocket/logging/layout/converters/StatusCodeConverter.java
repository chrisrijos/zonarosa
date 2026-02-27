/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.logging.layout.converters;

import io.zonarosa.websocket.logging.WebsocketEvent;

public class StatusCodeConverter extends WebSocketEventConverter {
  @Override
  public String convert(WebsocketEvent event) {
    if (event.getStatusCode() == WebsocketEvent.SENTINEL) {
      return WebsocketEvent.NA;
    } else {
      return Integer.toString(event.getStatusCode());
    }
  }
}
