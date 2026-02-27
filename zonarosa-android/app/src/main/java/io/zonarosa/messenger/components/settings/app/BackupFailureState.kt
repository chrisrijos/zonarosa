/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app

/**
 * Describes the current backup failure state.
 */
enum class BackupFailureState {
  NONE,
  BACKUP_FAILED,
  COULD_NOT_COMPLETE_BACKUP,
  SUBSCRIPTION_STATE_MISMATCH,
  ALREADY_REDEEMED,
  OUT_OF_STORAGE_SPACE
}
