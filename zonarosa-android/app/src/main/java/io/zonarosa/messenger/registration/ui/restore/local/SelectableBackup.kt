/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore.local

data class SelectableBackup(
  val timestamp: Long,
  val backupTime: String,
  val backupSize: String
)
