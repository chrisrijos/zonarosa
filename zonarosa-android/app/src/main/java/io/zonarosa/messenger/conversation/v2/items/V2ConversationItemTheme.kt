/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.items

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import io.zonarosa.messenger.R
import io.zonarosa.messenger.conversation.ConversationMessage
import io.zonarosa.messenger.conversation.v2.items.V2ConversationItemUtils.isThumbnailAtBottomOfBubble
import io.zonarosa.messenger.util.hasNoBubble
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Color information for conversation items.
 */
class V2ConversationItemTheme(
  private val context: Context,
  private val conversationContext: V2ConversationContext
) {

  @ColorInt
  fun getReplyIconBackgroundColor(): Int {
    return if (conversationContext.hasWallpaper()) {
      ContextCompat.getColor(context, CoreUiR.color.zonarosa_colorSurface1)
    } else {
      Color.TRANSPARENT
    }
  }

  @ColorInt
  fun getFooterForegroundColor(
    conversationMessage: ConversationMessage
  ): Int {
    if (conversationMessage.messageRecord.isThumbnailAtBottomOfBubble(context)) {
      return ContextCompat.getColor(context, CoreUiR.color.zonarosa_colorOnCustom)
    }

    if (conversationMessage.messageRecord.isOutgoing && conversationMessage.messageRecord.hasNoBubble(context) && !conversationContext.hasWallpaper()) {
      return conversationContext.getColorizer().getIncomingFooterTextColor(context, conversationContext.hasWallpaper())
    }

    return getColor(
      conversationMessage,
      conversationContext.getColorizer()::getOutgoingFooterTextColor,
      conversationContext.getColorizer()::getIncomingFooterTextColor
    )
  }

  @ColorInt
  fun getBodyTextColor(
    conversationMessage: ConversationMessage
  ): Int {
    return getColor(
      conversationMessage,
      conversationContext.getColorizer()::getOutgoingBodyTextColor,
      conversationContext.getColorizer()::getIncomingBodyTextColor
    )
  }

  @ColorInt
  fun getBodyBubbleColor(
    conversationMessage: ConversationMessage
  ): Int {
    if (conversationMessage.messageRecord.hasNoBubble(context)) {
      return Color.TRANSPARENT
    }

    return getFooterBubbleColor(conversationMessage)
  }

  @ColorInt
  fun getFooterBubbleColor(
    conversationMessage: ConversationMessage
  ): Int {
    return if (conversationMessage.messageRecord.isOutgoing) {
      Color.TRANSPARENT
    } else {
      if (conversationContext.hasWallpaper()) {
        ContextCompat.getColor(context, R.color.conversation_item_recv_bubble_color_wallpaper)
      } else {
        ContextCompat.getColor(context, R.color.conversation_item_recv_bubble_color_normal)
      }
    }
  }

  @ColorInt
  private fun getColor(
    conversationMessage: ConversationMessage,
    outgoingColor: (Context) -> Int,
    incomingColor: (Context, Boolean) -> Int
  ): Int {
    return if (conversationMessage.messageRecord.isOutgoing) {
      outgoingColor(context)
    } else {
      incomingColor(context, conversationContext.hasWallpaper())
    }
  }
}
