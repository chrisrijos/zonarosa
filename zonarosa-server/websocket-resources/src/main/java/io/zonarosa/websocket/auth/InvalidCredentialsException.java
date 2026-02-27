/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.auth;

public class InvalidCredentialsException extends Exception {

  public InvalidCredentialsException() {
    super(null, null, true, false);
  }
}
