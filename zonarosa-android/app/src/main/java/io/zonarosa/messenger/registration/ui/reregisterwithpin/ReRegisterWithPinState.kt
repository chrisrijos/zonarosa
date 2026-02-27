/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.reregisterwithpin

import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.lock.v2.PinKeyboardType

data class ReRegisterWithPinState(
  val isLocalVerification: Boolean = false,
  val hasIncorrectGuess: Boolean = false,
  val localPinMatches: Boolean = false,
  val pinKeyboardType: PinKeyboardType = ZonaRosaStore.pin.keyboardType
)
