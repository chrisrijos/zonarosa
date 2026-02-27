/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.backup;

public class BackupNotFoundException extends BackupException {

  public BackupNotFoundException(final String message) {
    super(message);
  }
}
