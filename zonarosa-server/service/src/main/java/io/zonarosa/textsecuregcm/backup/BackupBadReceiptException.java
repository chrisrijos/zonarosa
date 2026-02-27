/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.backup;

public class BackupBadReceiptException extends BackupException {

  public BackupBadReceiptException(String message) {
    super(message);
  }
}
