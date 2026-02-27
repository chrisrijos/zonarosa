package io.zonarosa.messenger.conversation.ui.inlinequery

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import io.zonarosa.messenger.components.mention.MentionAnnotation
import io.zonarosa.messenger.database.MentionUtil
import io.zonarosa.messenger.recipients.Recipient

/**
 * Encapsulate how to replace a query with a user selected result.
 */
sealed interface InlineQueryReplacement {
  fun toCharSequence(context: Context): CharSequence

  class Emoji(private val emoji: String) : InlineQueryReplacement {
    override fun toCharSequence(context: Context): CharSequence {
      return emoji
    }
  }

  class Mention(private val recipient: Recipient) : InlineQueryReplacement {
    override fun toCharSequence(context: Context): CharSequence {
      val builder = SpannableStringBuilder().apply {
        append(MentionUtil.MENTION_STARTER)
        append(recipient.getDisplayName(context))
        append(" ")
      }

      builder.setSpan(MentionAnnotation.mentionAnnotationForRecipientId(recipient.id), 0, builder.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

      return builder
    }
  }
}
