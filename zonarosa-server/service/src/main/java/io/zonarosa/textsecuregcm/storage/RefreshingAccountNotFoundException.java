/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

public class RefreshingAccountNotFoundException extends RuntimeException {

  public RefreshingAccountNotFoundException(final String message) {
    super(message);
  }

}
