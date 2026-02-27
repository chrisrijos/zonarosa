/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.olddevice

import io.zonarosa.messenger.registration.data.QuickRegistrationRepository
import io.zonarosa.service.api.provisioning.RestoreMethod

data class QuickTransferOldDeviceState(
  val reRegisterUri: String,
  val inProgress: Boolean = false,
  val reRegisterResult: QuickRegistrationRepository.TransferAccountResult? = null,
  val restoreMethodSelected: RestoreMethod? = null,
  val navigateToBackupCreation: Boolean = false,
  val lastBackupTimestamp: Long = 0,
  val performAuthentication: Boolean = false
)
