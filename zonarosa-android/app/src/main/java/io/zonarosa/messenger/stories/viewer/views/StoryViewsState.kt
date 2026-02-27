package io.zonarosa.messenger.stories.viewer.views

import io.zonarosa.messenger.recipients.Recipient

data class StoryViewsState(
  val loadState: LoadState = LoadState.INIT,
  val storyRecipient: Recipient? = null,
  val views: List<StoryViewItemData> = emptyList()
) {
  enum class LoadState {
    INIT,
    READY,
    DISABLED
  }
}
