/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.items

import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.RequestManager
import io.zonarosa.messenger.conversation.ConversationAdapter
import io.zonarosa.messenger.conversation.ConversationItemDisplayMode
import io.zonarosa.messenger.conversation.colors.Colorizer
import io.zonarosa.messenger.conversation.mutiselect.MultiselectPart
import io.zonarosa.messenger.database.model.MessageRecord

/**
 * Describes the Adapter "context" that would normally have been
 * visible to an inner class.
 */
interface V2ConversationContext {
  val lifecycleOwner: LifecycleOwner
  val requestManager: RequestManager
  val displayMode: ConversationItemDisplayMode
  val clickListener: ConversationAdapter.ItemClickListener
  val selectedItems: Set<MultiselectPart>
  val isMessageRequestAccepted: Boolean
  val searchQuery: String?
  val isParentInScroll: Boolean

  fun getChatColorsData(): ChatColorsDrawable.ChatColorsData

  fun onStartExpirationTimeout(messageRecord: MessageRecord)

  fun hasWallpaper(): Boolean
  fun getColorizer(): Colorizer
  fun getNextMessage(adapterPosition: Int): MessageRecord?
  fun getPreviousMessage(adapterPosition: Int): MessageRecord?
}
