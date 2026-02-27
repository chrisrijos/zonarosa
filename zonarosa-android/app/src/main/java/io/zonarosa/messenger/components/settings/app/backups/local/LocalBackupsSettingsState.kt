/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.components.settings.app.backups.local

/**
 * Immutable state for the on-device (legacy) backups settings screen.
 *
 * This is intended to be the single source of truth for UI rendering (i.e. a single `StateFlow`
 * emission fully describes what the screen should display).
 */
data class LocalBackupsSettingsState(
  val backupsEnabled: Boolean = false,
  val canTurnOn: Boolean = true,
  val lastBackupLabel: String? = null,
  val folderDisplayName: String? = null,
  val scheduleTimeLabel: String? = null,
  val progress: BackupProgressState = BackupProgressState.Idle
)
