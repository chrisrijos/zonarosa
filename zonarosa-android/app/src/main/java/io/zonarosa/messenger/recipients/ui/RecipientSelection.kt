/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.recipients.ui

import io.zonarosa.messenger.recipients.PhoneNumber
import io.zonarosa.messenger.recipients.RecipientId

sealed interface RecipientSelection {
  sealed interface HasId : RecipientSelection {
    val id: RecipientId
  }

  sealed interface HasPhone : RecipientSelection {
    val phone: PhoneNumber
  }

  data class WithId(override val id: RecipientId) : HasId
  data class WithPhone(override val phone: PhoneNumber) : HasPhone
  data class WithIdAndPhone(override val id: RecipientId, override val phone: PhoneNumber) : HasId, HasPhone
}
