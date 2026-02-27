package io.zonarosa.messenger.badges.gifts.viewgift.sent

import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.recipients.Recipient

data class ViewSentGiftState(
  val recipient: Recipient? = null,
  val badge: Badge? = null
)
