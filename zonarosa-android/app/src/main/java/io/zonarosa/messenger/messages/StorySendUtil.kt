package io.zonarosa.messenger.messages

import io.zonarosa.core.util.Base64
import io.zonarosa.messenger.database.model.databaseprotos.StoryTextPost
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.service.api.messages.ZonaRosaServicePreview
import io.zonarosa.service.api.messages.ZonaRosaServiceTextAttachment
import java.io.IOException
import java.util.Optional
import kotlin.math.roundToInt

object StorySendUtil {
  @JvmStatic
  @Throws(IOException::class)
  fun deserializeBodyToStoryTextAttachment(message: OutgoingMessage, getPreviewsFor: (OutgoingMessage) -> List<ZonaRosaServicePreview>): ZonaRosaServiceTextAttachment {
    val storyTextPost = StoryTextPost.ADAPTER.decode(Base64.decode(message.body))
    val preview = if (message.linkPreviews.isEmpty()) {
      Optional.empty()
    } else {
      Optional.of(getPreviewsFor(message)[0])
    }

    return if (storyTextPost.background!!.linearGradient != null) {
      ZonaRosaServiceTextAttachment.forGradientBackground(
        Optional.ofNullable(storyTextPost.body),
        Optional.ofNullable(getStyle(storyTextPost.style)),
        Optional.of(storyTextPost.textForegroundColor),
        Optional.of(storyTextPost.textBackgroundColor),
        preview,
        ZonaRosaServiceTextAttachment.Gradient(
          Optional.of(storyTextPost.background.linearGradient!!.rotation.roundToInt()),
          ArrayList(storyTextPost.background.linearGradient.colors),
          ArrayList(storyTextPost.background.linearGradient.positions)
        )
      )
    } else {
      ZonaRosaServiceTextAttachment.forSolidBackground(
        Optional.ofNullable(storyTextPost.body),
        Optional.ofNullable(getStyle(storyTextPost.style)),
        Optional.of(storyTextPost.textForegroundColor),
        Optional.of(storyTextPost.textBackgroundColor),
        preview,
        storyTextPost.background.singleColor!!.color
      )
    }
  }

  private fun getStyle(style: StoryTextPost.Style): ZonaRosaServiceTextAttachment.Style {
    return when (style) {
      StoryTextPost.Style.REGULAR -> ZonaRosaServiceTextAttachment.Style.REGULAR
      StoryTextPost.Style.BOLD -> ZonaRosaServiceTextAttachment.Style.BOLD
      StoryTextPost.Style.SERIF -> ZonaRosaServiceTextAttachment.Style.SERIF
      StoryTextPost.Style.SCRIPT -> ZonaRosaServiceTextAttachment.Style.SCRIPT
      StoryTextPost.Style.CONDENSED -> ZonaRosaServiceTextAttachment.Style.CONDENSED
      else -> ZonaRosaServiceTextAttachment.Style.DEFAULT
    }
  }
}
