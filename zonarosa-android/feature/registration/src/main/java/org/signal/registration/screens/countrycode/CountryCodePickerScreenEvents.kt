/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.countrycode

sealed interface CountryCodePickerScreenEvents {
  data class Search(val query: String) : CountryCodePickerScreenEvents
  data class CountrySelected(val country: Country) : CountryCodePickerScreenEvents
  data object Dismissed : CountryCodePickerScreenEvents
}
