package io.zonarosa.messenger.stories.settings.group

import io.zonarosa.messenger.recipients.Recipient

data class GroupStorySettingsState(
  val name: String = "",
  val members: List<Recipient> = emptyList(),
  val removed: Boolean = false
)
