package io.zonarosa.messenger.stories.viewer.page

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.zonarosa.messenger.recipients.RecipientId

@Parcelize
data class StoryViewerPageArgs(
  val recipientId: RecipientId,
  val initialStoryId: Long,
  val isOutgoingOnly: Boolean,
  val isJumpForwardToUnviewed: Boolean,
  val source: Source,
  val groupReplyStartPosition: Int
) : Parcelable {
  enum class Source {
    UNKNOWN,
    NOTIFICATION,
    INFO_CONTEXT
  }
}
