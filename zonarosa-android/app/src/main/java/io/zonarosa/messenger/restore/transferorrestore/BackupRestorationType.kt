/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.restore.transferorrestore

/**
 *  What kind of backup restore the user wishes to perform.
 */
enum class BackupRestorationType {
  DEVICE_TRANSFER,
  LOCAL_BACKUP,
  NONE
}
