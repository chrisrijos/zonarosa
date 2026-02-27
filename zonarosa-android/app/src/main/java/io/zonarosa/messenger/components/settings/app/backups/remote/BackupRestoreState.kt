/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.backups.remote

import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgressState

/**
 * State container for BackupStatusData, including the enabled state.
 */
sealed interface BackupRestoreState {
  data object None : BackupRestoreState
  data class Ready(val bytes: String) : BackupRestoreState
  data class Restoring(val state: ArchiveRestoreProgressState) : BackupRestoreState
}
