/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.screens.pinsettings

data class PinSettingsState(
  val hasPinSet: Boolean = false,
  val registrationLockEnabled: Boolean = false,
  val pinsOptedOut: Boolean = false,
  val loading: Boolean = false,
  val toastMessage: String? = null
)
