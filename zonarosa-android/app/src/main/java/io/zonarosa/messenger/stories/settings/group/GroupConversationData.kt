package io.zonarosa.messenger.stories.settings.group

import io.zonarosa.messenger.recipients.RecipientId

/**
 * Minimum data needed to launch ConversationActivity for a given grou
 */
data class GroupConversationData(
  val groupRecipientId: RecipientId,
  val groupThreadId: Long
)
