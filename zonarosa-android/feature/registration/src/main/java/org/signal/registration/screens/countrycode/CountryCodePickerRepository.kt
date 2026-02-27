/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.countrycode

import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.E164Util
import java.text.Collator
import java.util.Locale

/**
 * Repository for fetching country data used by the country code picker.
 */
class CountryCodePickerRepository {

  companion object {
    /** A hardcoded list of countries to suggest during registration. Can change at any time. */
    private val COMMON_COUNTRIES = listOf("US", "DE", "IN", "NL", "UA")
  }

  suspend fun getCountries(): List<Country> = withContext(Dispatchers.IO) {
    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY

    PhoneNumberUtil.getInstance().supportedRegions
      .map { region ->
        Country(
          name = E164Util.getRegionDisplayName(region).orElse(""),
          emoji = countryToEmoji(region),
          countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(region),
          regionCode = region
        )
      }.sortedWith { lhs, rhs ->
        collator.compare(lhs.name.lowercase(Locale.getDefault()), rhs.name.lowercase(Locale.getDefault()))
      }
  }

  suspend fun getCommonCountries(): List<Country> = withContext(Dispatchers.IO) {
    COMMON_COUNTRIES.map { region ->
      Country(
        name = E164Util.getRegionDisplayName(region).orElse(""),
        emoji = countryToEmoji(region),
        countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(region),
        regionCode = region
      )
    }
  }

  private fun countryToEmoji(countryCode: String): String {
    return if (countryCode.isNotEmpty()) {
      countryCode
        .uppercase(Locale.US)
        .map { char -> Character.codePointAt("$char", 0) - 0x41 + 0x1F1E6 }
        .map { codePoint -> Character.toChars(codePoint) }
        .joinToString(separator = "") { charArray -> String(charArray) }
    } else {
      ""
    }
  }
}
