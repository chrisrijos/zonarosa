/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.websocket.messages;

public class InvalidMessageException extends Exception {
  public InvalidMessageException(String s) {
    super(s);
  }

  public InvalidMessageException(Exception e) {
    super(e);
  }
}
