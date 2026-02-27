/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.messages;


import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface WebSocketMessageFactory {

  public WebSocketMessage parseMessage(byte[] serialized, int offset, int len)
      throws InvalidMessageException;

  public WebSocketMessage createRequest(Optional<Long> requestId,
                                        String verb, String path,
                                        List<String> headers,
                                        Optional<byte[]> body);

  public WebSocketMessage createResponse(long requestId, int status, String message,
                                         List<String> headers,
                                         Optional<byte[]> body);

}
