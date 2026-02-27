/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import io.zonarosa.websocket.session.ContextPrincipal;
import io.zonarosa.websocket.session.WebSocketSessionContext;

public class WebSocketSecurityContext implements SecurityContext {

  private final ContextPrincipal principal;

  public WebSocketSecurityContext(ContextPrincipal principal) {
    this.principal = principal;
  }

  @Override
  public Principal getUserPrincipal() {
    return (Principal)principal.getContext().getAuthenticated();
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public boolean isSecure() {
    return principal != null;
  }

  @Override
  public String getAuthenticationScheme() {
    return null;
  }

  public WebSocketSessionContext getSessionContext() {
    return principal.getContext();
  }
}
