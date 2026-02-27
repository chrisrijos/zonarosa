/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.screens.pinsettings

sealed interface PinSettingsEvents {
  data class SetPin(val pin: String) : PinSettingsEvents
  data object ToggleRegistrationLock : PinSettingsEvents
  data object TogglePinsOptOut : PinSettingsEvents
  data object Back : PinSettingsEvents
  data object DismissMessage : PinSettingsEvents
}
