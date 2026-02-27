/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.messages;


import java.util.Map;
import java.util.Optional;

public interface WebSocketResponseMessage {
  public long               getRequestId();
  public int                getStatus();
  public String             getMessage();
  public Map<String,String> getHeaders();
  public Optional<byte[]> getBody();
}
