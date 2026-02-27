package io.zonarosa.messenger.reactions

import io.zonarosa.messenger.recipients.Recipient

/**
 * A UI model for a reaction in the [ReactionsBottomSheetDialogFragment]
 */
data class ReactionDetails(
  val sender: Recipient,
  val baseEmoji: String,
  val displayEmoji: String,
  val timestamp: Long
)
