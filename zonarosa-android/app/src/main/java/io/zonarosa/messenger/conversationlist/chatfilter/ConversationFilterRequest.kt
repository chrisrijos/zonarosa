package io.zonarosa.messenger.conversationlist.chatfilter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.zonarosa.messenger.conversationlist.model.ConversationFilter

@Parcelize
data class ConversationFilterRequest(
  val filter: ConversationFilter,
  val source: ConversationFilterSource
) : Parcelable
