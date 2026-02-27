/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.util

data class CountryPrefix(val digits: Int, val regionCode: String) {
  override fun toString(): String {
    return "+$digits"
  }
}
