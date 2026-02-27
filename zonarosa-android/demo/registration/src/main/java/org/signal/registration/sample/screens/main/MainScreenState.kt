/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.screens.main

data class MainScreenState(
  val existingRegistrationState: ExistingRegistrationState? = null
) {
  data class ExistingRegistrationState(
    val phoneNumber: String,
    val aci: String,
    val pni: String,
    val aep: String,
    val pin: String?,
    val registrationLockEnabled: Boolean,
    val pinsOptedOut: Boolean,
    val temporaryMasterKey: String?
  )
}
