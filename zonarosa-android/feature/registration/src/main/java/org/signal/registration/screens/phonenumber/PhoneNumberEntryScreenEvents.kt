/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.phonenumber

sealed interface PhoneNumberEntryScreenEvents {
  data class CountryCodeChanged(val value: String) : PhoneNumberEntryScreenEvents
  data class PhoneNumberChanged(val value: String) : PhoneNumberEntryScreenEvents
  data class CountrySelected(val countryCode: Int, val regionCode: String, val countryName: String, val countryEmoji: String) : PhoneNumberEntryScreenEvents
  data object PhoneNumberSubmitted : PhoneNumberEntryScreenEvents
  data object CountryPicker : PhoneNumberEntryScreenEvents
  data class CaptchaCompleted(val token: String) : PhoneNumberEntryScreenEvents
  data object ConsumeOneTimeEvent : PhoneNumberEntryScreenEvents
}
