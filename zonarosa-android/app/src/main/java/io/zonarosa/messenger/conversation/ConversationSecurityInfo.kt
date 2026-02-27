package io.zonarosa.messenger.conversation

import io.zonarosa.messenger.recipients.RecipientId

data class ConversationSecurityInfo(
  val recipientId: RecipientId = RecipientId.UNKNOWN,
  val isPushAvailable: Boolean = false,
  val isDefaultSmsApplication: Boolean = false,
  val isInitialized: Boolean = false,
  val hasUnexportedInsecureMessages: Boolean = false,
  val isClientExpired: Boolean = false,
  val isUnauthorized: Boolean = false
)
