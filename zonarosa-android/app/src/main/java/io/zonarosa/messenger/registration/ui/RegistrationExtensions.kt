/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

fun PhoneNumber.toE164(): String {
  return PhoneNumberUtil.getInstance().format(this, PhoneNumberUtil.PhoneNumberFormat.E164)
}
