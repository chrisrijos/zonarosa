/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.messages;

public interface WebSocketMessage {

  public enum Type {
    UNKNOWN_MESSAGE,
    REQUEST_MESSAGE,
    RESPONSE_MESSAGE
  }

  public Type                     getType();
  public WebSocketRequestMessage  getRequestMessage();
  public WebSocketResponseMessage getResponseMessage();
  public byte[]                   toByteArray();

}
