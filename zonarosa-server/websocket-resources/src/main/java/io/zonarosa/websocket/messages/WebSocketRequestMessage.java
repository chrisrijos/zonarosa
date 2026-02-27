/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.messages;

import java.util.Map;
import java.util.Optional;

public interface WebSocketRequestMessage {

  public String             getVerb();
  public String             getPath();
  public Map<String,String> getHeaders();
  public Optional<byte[]> getBody();
  public long               getRequestId();
  public boolean            hasRequestId();

}
