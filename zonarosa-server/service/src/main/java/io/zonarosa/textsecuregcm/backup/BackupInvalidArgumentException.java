/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.backup;

public class BackupInvalidArgumentException extends BackupException {
  public BackupInvalidArgumentException(final String message) {
    super(message);
  }
}
