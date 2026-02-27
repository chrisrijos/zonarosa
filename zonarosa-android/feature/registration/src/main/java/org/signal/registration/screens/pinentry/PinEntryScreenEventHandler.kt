/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.pinentry

object PinEntryScreenEventHandler {

  fun applyEvent(state: PinEntryState, event: PinEntryScreenEvents): PinEntryState {
    return when (event) {
      PinEntryScreenEvents.ToggleKeyboard -> state.copy(isAlphanumericKeyboard = !state.isAlphanumericKeyboard)
      else -> throw UnsupportedOperationException("This even is not handled generically!")
    }
  }
}
