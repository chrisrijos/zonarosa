/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.screens.main

sealed interface MainScreenEvents {
  data object LaunchRegistration : MainScreenEvents
  data object OpenPinSettings : MainScreenEvents
  data object ClearAllData : MainScreenEvents
}
