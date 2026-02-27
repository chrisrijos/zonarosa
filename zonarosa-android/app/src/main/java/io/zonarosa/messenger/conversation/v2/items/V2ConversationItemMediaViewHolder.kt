/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.items

import android.util.TypedValue
import android.view.View
import io.zonarosa.messenger.components.QuoteView
import io.zonarosa.messenger.conversation.v2.data.ConversationMessageElement
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.util.adapter.mapping.MappingModel
import io.zonarosa.messenger.util.changeConstraints
import io.zonarosa.messenger.util.isStoryReaction
import io.zonarosa.messenger.util.visible

/**
 * Represents a media-backed conversation item.
 */
class V2ConversationItemMediaViewHolder<Model : MappingModel<Model>>(
  private val binding: V2ConversationItemMediaBindingBridge,
  private val conversationContext: V2ConversationContext
) : V2ConversationItemTextOnlyViewHolder<Model>(
  binding.textBridge,
  conversationContext,
  V2FooterPositionDelegate(binding)
) {

  init {
    binding.textBridge.bodyWrapper.clipToOutline = true
  }

  override fun bind(model: Model) {
    conversationMessage = (model as ConversationMessageElement).conversationMessage

    if (payload.isNotEmpty()) {
      super.bind(model)
      return
    }

    presentThumbnail()
    super.bind(model)
    presentQuote()
    updateMediaConstraints()
  }

  private fun updateMediaConstraints() {
    binding.bodyContentSpacer.visible = (hasGroupSenderName() && hasThumbnail()) || hasQuote()

    binding.textBridge.root.changeConstraints {
      val maxBodyWidth = if (hasThumbnail()) {
        binding.thumbnailStub.get().thumbWidth
      } else {
        0
      }

      this.constrainMaxWidth(binding.textBridge.bodyWrapper.id, maxBodyWidth)
    }
  }

  private fun presentQuote() {
    val record = requireMediaMessage()
    val quote = record.quote

    if (quote == null) {
      binding.quoteStub.visibility = View.GONE
      return
    }

    val quoteView = binding.quoteStub.get()
    quoteView.setOnClickListener {
      if (conversationContext.selectedItems.isEmpty()) {
        conversationContext.clickListener.onQuoteClicked(record)
      } else {
        conversationContext.clickListener.onItemClick(getMultiselectPartForLatestTouch())
      }
    }

    binding.quoteStub.visibility = View.VISIBLE
    quoteView.setQuote(
      conversationContext.requestManager,
      quote.id,
      Recipient.live(quote.author).get(),
      quote.displayText,
      quote.isOriginalMissing,
      quote.attachment,
      if (conversationMessage.messageRecord.isStoryReaction()) conversationMessage.messageRecord.body else null,
      quote.quoteType,
      false,
      conversationMessage.quoteMemberLabel
    )

    quoteView.setMessageType(
      when {
        conversationMessage.messageRecord.isStoryReaction() && binding.textBridge.isIncoming -> QuoteView.MessageType.STORY_REPLY_INCOMING
        conversationMessage.messageRecord.isStoryReaction() -> QuoteView.MessageType.STORY_REPLY_OUTGOING
        binding.textBridge.isIncoming -> QuoteView.MessageType.INCOMING
        else -> QuoteView.MessageType.OUTGOING
      }
    )

    quoteView.setWallpaperEnabled(conversationContext.hasWallpaper())
    quoteView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ZonaRosaStore.settings.getMessageQuoteFontSize(context).toFloat())

    val isOutgoing = conversationMessage.messageRecord.isOutgoing
    when (shape) {
      V2ConversationItemShape.MessageShape.SINGLE, V2ConversationItemShape.MessageShape.START -> {
        val isGroupThread = conversationMessage.threadRecipient.isGroup
        quoteView.setTopCornerSizes(
          isOutgoing || !isGroupThread,
          isOutgoing || !isGroupThread
        )
      }

      V2ConversationItemShape.MessageShape.MIDDLE, V2ConversationItemShape.MessageShape.END -> {
        quoteView.setTopCornerSizes(isOutgoing, !isOutgoing)
      }
    }
  }

  private fun presentThumbnail() {
    if (binding.thumbnailStub.resolved() || requireMediaMessage().slideDeck.thumbnailSlides.size == 1) {
      binding.thumbnailStub.get().presentThumbnail(
        mediaMessage = requireMediaMessage(),
        conversationContext = conversationContext
      )
    }
  }

  private fun hasGroupSenderName(): Boolean {
    return binding.textBridge.senderNameWithLabel?.visible == true
  }

  private fun hasThumbnail(): Boolean {
    return binding.thumbnailStub.isVisible
  }

  private fun hasQuote(): Boolean {
    return binding.quoteStub.isVisible
  }

  private fun hasMedia(): Boolean {
    return hasThumbnail() || hasQuote()
  }

  private fun requireMediaMessage(): MmsMessageRecord {
    return conversationMessage.messageRecord as MmsMessageRecord
  }
}
