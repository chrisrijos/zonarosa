/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.pincreation

import io.zonarosa.core.models.AccountEntropyPool

data class PinCreationState(
  val isAlphanumericKeyboard: Boolean = false,
  val inputLabel: String? = null,
  val isConfirmEnabled: Boolean = false,
  val accountEntropyPool: AccountEntropyPool? = null
)
