/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.olddevice.preparedevice

/**
 * Events emitted by the PrepareDevice screen.
 */
sealed interface PrepareDeviceScreenEvents {
  data object NavigateBack : PrepareDeviceScreenEvents
  data object BackUpNow : PrepareDeviceScreenEvents
  data object SkipAndContinue : PrepareDeviceScreenEvents
}
