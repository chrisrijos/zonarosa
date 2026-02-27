/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.storage.devicecheck;

public class RequestReuseException extends Exception {

  public RequestReuseException(String s) {
    super(s);
  }
}
