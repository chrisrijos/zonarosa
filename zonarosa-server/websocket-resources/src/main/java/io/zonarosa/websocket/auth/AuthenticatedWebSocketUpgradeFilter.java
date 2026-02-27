/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.websocket.auth;

import java.security.Principal;
import java.util.Optional;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;

public interface AuthenticatedWebSocketUpgradeFilter<T extends Principal> {

  void handleAuthentication(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<T> authenticated,
      JettyServerUpgradeRequest request,
      JettyServerUpgradeResponse response);
}
