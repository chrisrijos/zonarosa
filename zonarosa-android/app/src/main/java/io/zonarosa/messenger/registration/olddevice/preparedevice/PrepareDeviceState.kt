/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.olddevice.preparedevice

/**
 * State for the PrepareDevice screen shown during quick restore flow.
 *
 * @param lastBackupTimestamp The timestamp of the last backup in milliseconds, or 0 if never backed up.
 */
data class PrepareDeviceState(
  val lastBackupTimestamp: Long = 0
)
