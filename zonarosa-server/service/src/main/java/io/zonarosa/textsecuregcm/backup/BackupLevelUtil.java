/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.backup;

import io.zonarosa.libzonarosa.zkgroup.backups.BackupLevel;

public class BackupLevelUtil {
  public static BackupLevel fromReceiptLevel(long receiptLevel) {
    try {
      return BackupLevel.fromValue(Math.toIntExact(receiptLevel));
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Invalid receipt level: " + receiptLevel);
    }
  }
}
