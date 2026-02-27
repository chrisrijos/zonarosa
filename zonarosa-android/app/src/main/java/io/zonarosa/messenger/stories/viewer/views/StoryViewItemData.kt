package io.zonarosa.messenger.stories.viewer.views

import io.zonarosa.messenger.recipients.Recipient

data class StoryViewItemData(
  val recipient: Recipient,
  val timeViewedInMillis: Long
)
