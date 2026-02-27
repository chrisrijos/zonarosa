/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data

data class RegisterAsLinkedDeviceResponse(
  val deviceId: Int,
  val accountRegistrationResult: AccountRegistrationResult
)
