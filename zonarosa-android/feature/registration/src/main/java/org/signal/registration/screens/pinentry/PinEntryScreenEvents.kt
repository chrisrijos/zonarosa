/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.pinentry

sealed class PinEntryScreenEvents {
  data class PinEntered(val pin: String) : PinEntryScreenEvents()
  data object ToggleKeyboard : PinEntryScreenEvents()
  data object NeedHelp : PinEntryScreenEvents()
  data object Skip : PinEntryScreenEvents()
}
