/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.ui.edit

import io.zonarosa.messenger.conversation.ConversationAdapter
import io.zonarosa.messenger.conversation.ConversationBottomSheetCallback
import io.zonarosa.messenger.conversation.ConversationMessage
import io.zonarosa.messenger.database.model.MessageRecord

object EmptyConversationBottomSheetCallback : ConversationBottomSheetCallback {
  override fun getConversationAdapterListener(): ConversationAdapter.ItemClickListener = EmptyConversationAdapterListener
  override fun jumpToMessage(messageRecord: MessageRecord) = Unit
  override fun unpin(conversationMessage: ConversationMessage) = Unit
  override fun copy(conversationMessage: ConversationMessage) = Unit
  override fun delete(conversationMessage: ConversationMessage) = Unit
  override fun save(conversationMessage: ConversationMessage) = Unit
}
