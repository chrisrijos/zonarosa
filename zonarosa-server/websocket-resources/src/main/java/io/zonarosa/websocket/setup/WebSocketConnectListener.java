/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.setup;

import io.zonarosa.websocket.session.WebSocketSessionContext;

public interface WebSocketConnectListener {
  public void onWebSocketConnect(WebSocketSessionContext context);
}
