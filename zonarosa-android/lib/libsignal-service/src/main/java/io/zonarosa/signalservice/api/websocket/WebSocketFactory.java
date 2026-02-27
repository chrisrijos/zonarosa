package io.zonarosa.service.api.websocket;

import io.zonarosa.service.internal.websocket.WebSocketConnection;

public interface WebSocketFactory {
  WebSocketConnection createConnection() throws WebSocketUnavailableException;
}
