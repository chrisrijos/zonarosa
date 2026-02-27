package io.zonarosa.messenger.stories.viewer.reply.group

import io.zonarosa.messenger.conversation.colors.NameColor
import io.zonarosa.messenger.recipients.RecipientId

data class StoryGroupReplyState(
  val threadId: Long = 0L,
  val replies: List<ReplyBody> = emptyList(),
  val nameColors: Map<RecipientId, NameColor> = emptyMap(),
  val loadState: LoadState = LoadState.INIT
) {
  val noReplies: Boolean = replies.isEmpty()

  enum class LoadState {
    INIT,
    READY
  }
}
