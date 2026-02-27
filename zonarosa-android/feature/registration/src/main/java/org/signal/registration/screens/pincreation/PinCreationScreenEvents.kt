/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.pincreation

sealed class PinCreationScreenEvents {
  data class PinSubmitted(val pin: String) : PinCreationScreenEvents()
  data object ToggleKeyboard : PinCreationScreenEvents()
  data object LearnMore : PinCreationScreenEvents()
}
