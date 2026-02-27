/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

/**
 * A disconnection request listener receives and handles a request to close an authenticated network connection for a
 * specific client.
 */
public interface DisconnectionRequestListener {

  /**
   * Handles a request to close an authenticated network connection for a specific authenticated device. Requests are
   * dispatched on dedicated threads, and implementations may safely block.
   */
  void handleDisconnectionRequest();
}
