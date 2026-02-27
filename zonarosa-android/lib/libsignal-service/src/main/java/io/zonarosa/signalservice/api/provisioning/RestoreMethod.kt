/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.provisioning

/**
 * Restore method chosen by user on new device after performing a quick-restore.
 */
enum class RestoreMethod {
  REMOTE_BACKUP,
  LOCAL_BACKUP,
  DEVICE_TRANSFER,
  DECLINE
}
