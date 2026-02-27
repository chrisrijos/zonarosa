/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.recipients

import io.zonarosa.messenger.util.ZonaRosaE164Util

@JvmInline
value class PhoneNumber(val value: String) {
  val displayText: String
    get() = ZonaRosaE164Util.prettyPrint(value)
}
