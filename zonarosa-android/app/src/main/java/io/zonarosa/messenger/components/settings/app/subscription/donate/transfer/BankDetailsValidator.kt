/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.donate.transfer

object BankDetailsValidator {

  private val EMAIL_REGEX: Regex = ".+@.+\\..+".toRegex()

  fun validName(name: String): Boolean {
    return name.length >= 3
  }

  fun validEmail(email: String): Boolean {
    return email.length >= 3 && email.matches(EMAIL_REGEX)
  }
}
