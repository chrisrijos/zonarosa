/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.countrycode

/**
 * State managed by [CountryCodePickerViewModel]. Includes country list and allows for searching
 */
data class CountryCodeState(
  val query: String = "",
  val countryList: List<Country> = emptyList(),
  val commonCountryList: List<Country> = emptyList(),
  val filteredList: List<Country> = emptyList(),
  val startingIndex: Int = 0
)
