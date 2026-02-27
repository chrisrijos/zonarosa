/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore.local

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface RestoreLocalBackupNavKey : NavKey {
  @Serializable
  object SelectLocalBackupTypeScreen : RestoreLocalBackupNavKey

  @Serializable
  object FolderInstructionSheet : RestoreLocalBackupNavKey

  @Serializable
  object FileInstructionSheet : RestoreLocalBackupNavKey

  @Serializable
  object SelectLocalBackupScreen : RestoreLocalBackupNavKey

  @Serializable
  object SelectLocalBackupSheet : RestoreLocalBackupNavKey

  @Serializable
  object EnterLocalBackupKeyScreen : RestoreLocalBackupNavKey

  @Serializable
  object NoRecoveryKeySheet : RestoreLocalBackupNavKey
}
