/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.phonenumber

import io.zonarosa.registration.NetworkController
import io.zonarosa.registration.NetworkController.SessionMetadata
import io.zonarosa.registration.PreExistingRegistrationData
import kotlin.time.Duration

data class PhoneNumberEntryState(
  val regionCode: String = "US",
  val countryCode: String = "1",
  val countryName: String = "United States",
  val countryEmoji: String = "\uD83C\uDDFA\uD83C\uDDF8",
  val nationalNumber: String = "",
  val formattedNumber: String = "",
  val sessionE164: String? = null,
  val sessionMetadata: SessionMetadata? = null,
  val showSpinner: Boolean = false,
  val oneTimeEvent: OneTimeEvent? = null,
  val preExistingRegistrationData: PreExistingRegistrationData? = null,
  val restoredSvrCredentials: List<NetworkController.SvrCredentials> = emptyList()
) {
  sealed interface OneTimeEvent {
    data object NetworkError : OneTimeEvent
    data object UnknownError : OneTimeEvent
    data class RateLimited(val retryAfter: Duration) : OneTimeEvent
    data object ThirdPartyError : OneTimeEvent
    data object CouldNotRequestCodeWithSelectedTransport : OneTimeEvent
  }
}
