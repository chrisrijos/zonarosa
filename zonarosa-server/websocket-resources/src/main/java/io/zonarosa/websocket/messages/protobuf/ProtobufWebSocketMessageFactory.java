/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.messages.protobuf;

import com.google.protobuf.ByteString;
import io.zonarosa.websocket.messages.InvalidMessageException;
import io.zonarosa.websocket.messages.WebSocketMessage;
import io.zonarosa.websocket.messages.WebSocketMessageFactory;

import java.util.List;
import java.util.Optional;

public class ProtobufWebSocketMessageFactory implements WebSocketMessageFactory {

  @Override
  public WebSocketMessage parseMessage(byte[] serialized, int offset, int len)
      throws InvalidMessageException
  {
    return new ProtobufWebSocketMessage(serialized, offset, len);
  }

  @Override
  public WebSocketMessage createRequest(Optional<Long> requestId,
                                        String verb, String path,
                                        List<String> headers,
                                        Optional<byte[]> body)
  {
    SubProtocol.WebSocketRequestMessage.Builder requestMessage =
        SubProtocol.WebSocketRequestMessage.newBuilder()
                                           .setVerb(verb)
                                           .setPath(path);

    if (requestId.isPresent()) {
      requestMessage.setId(requestId.get());
    }

    if (body.isPresent()) {
      requestMessage.setBody(ByteString.copyFrom(body.get()));
    }

    if (headers != null) {
      requestMessage.addAllHeaders(headers);
    }

    SubProtocol.WebSocketMessage message
        = SubProtocol.WebSocketMessage.newBuilder()
                                      .setType(SubProtocol.WebSocketMessage.Type.REQUEST)
                                      .setRequest(requestMessage)
                                      .build();

    return new ProtobufWebSocketMessage(message);
  }

  @Override
  public WebSocketMessage createResponse(long requestId, int status, String messageString, List<String> headers, Optional<byte[]> body) {
    SubProtocol.WebSocketResponseMessage.Builder responseMessage =
        SubProtocol.WebSocketResponseMessage.newBuilder()
                                            .setId(requestId)
                                            .setStatus(status)
                                            .setMessage(messageString);

    if (body.isPresent()) {
      responseMessage.setBody(ByteString.copyFrom(body.get()));
    }

    if (headers != null) {
      responseMessage.addAllHeaders(headers);
    }

    SubProtocol.WebSocketMessage message =
        SubProtocol.WebSocketMessage.newBuilder()
                                    .setType(SubProtocol.WebSocketMessage.Type.RESPONSE)
                                    .setResponse(responseMessage)
                                    .build();

    return new ProtobufWebSocketMessage(message);
  }
}
