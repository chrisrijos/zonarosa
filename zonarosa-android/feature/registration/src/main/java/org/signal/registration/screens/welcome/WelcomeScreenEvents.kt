/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.welcome

sealed class WelcomeScreenEvents {
  data object Continue : WelcomeScreenEvents()
  data object HasOldPhone : WelcomeScreenEvents()
  data object DoesNotHaveOldPhone : WelcomeScreenEvents()
}
