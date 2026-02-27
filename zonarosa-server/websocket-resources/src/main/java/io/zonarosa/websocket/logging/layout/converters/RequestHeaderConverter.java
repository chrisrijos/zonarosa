/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.logging.layout.converters;

import io.zonarosa.websocket.logging.WebsocketEvent;

import ch.qos.logback.core.util.OptionHelper;

public class RequestHeaderConverter extends WebSocketEventConverter {

  private String key;

  @Override
  public void start() {
    key = getFirstOption();
    if (OptionHelper.isEmpty(key)) {
      addWarn("Missing key for the requested header. Defaulting to all keys.");
      key = null;
    }
    super.start();
  }

  @Override
  public String convert(WebsocketEvent websocketEvent) {
    if (!isStarted()) {
      return "INACTIVE_HEADER_CONV";
    }

    if (key != null) {
      return websocketEvent.getRequestHeader(key);
    } else {
      return websocketEvent.getRequestHeaderMap().toString();
    }
  }
}
