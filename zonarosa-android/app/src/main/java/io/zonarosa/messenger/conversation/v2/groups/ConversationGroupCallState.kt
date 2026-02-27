/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.groups

import io.zonarosa.messenger.recipients.RecipientId

/** State of a group call used solely within rendering UX/UI in the conversation */
data class ConversationGroupCallState(
  val recipientId: RecipientId? = null,
  val activeV2Group: Boolean = false,
  val ongoingCall: Boolean = false,
  val hasCapacity: Boolean = false
)
