package io.zonarosa.messenger.stories.viewer.reply.direct

import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.recipients.Recipient

data class StoryDirectReplyState(
  val groupDirectReplyRecipient: Recipient? = null,
  val storyRecord: MessageRecord? = null
)
