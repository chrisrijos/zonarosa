package io.zonarosa.messenger.database.model

import io.zonarosa.messenger.recipients.RecipientId

data class DistributionListPartialRecord(
  val id: DistributionListId,
  val name: CharSequence,
  val recipientId: RecipientId,
  val allowsReplies: Boolean,
  val isUnknown: Boolean,
  val privacyMode: DistributionListPrivacyMode
)
