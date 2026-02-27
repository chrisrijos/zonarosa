/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.session;

import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ContainerRequest;
import io.zonarosa.websocket.WebSocketSecurityContext;

public class WebSocketSessionContainerRequestValueFactory {
  private final ContainerRequest request;

  public WebSocketSessionContainerRequestValueFactory(ContainerRequest request) {
    this.request = request;
  }

  public WebSocketSessionContext provide() {
    SecurityContext securityContext = request.getSecurityContext();

    if (!(securityContext instanceof WebSocketSecurityContext)) {
      throw new IllegalStateException("Security context isn't for websocket!");
    }

    WebSocketSessionContext sessionContext = ((WebSocketSecurityContext)securityContext).getSessionContext();

    if (sessionContext == null) {
      throw new IllegalStateException("No session context found for websocket!");
    }

    return sessionContext;
  }

}
