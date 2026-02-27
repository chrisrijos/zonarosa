/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.accountlocked

sealed class AccountLockedScreenEvents {
  data object Next : AccountLockedScreenEvents()
  data object LearnMore : AccountLockedScreenEvents()
}
