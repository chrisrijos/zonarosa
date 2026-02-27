/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.recipients.ui.findby

import io.zonarosa.messenger.recipients.RecipientId

sealed interface FindByResult {
  data class Success(val recipientId: RecipientId) : FindByResult
  object InvalidEntry : FindByResult
  data class NotFound(val recipientId: RecipientId = RecipientId.UNKNOWN) : FindByResult
  object NetworkError : FindByResult
}
