/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.items

import android.widget.Space
import io.zonarosa.core.ui.view.Stub
import io.zonarosa.messenger.components.QuoteView
import io.zonarosa.messenger.databinding.V2ConversationItemMediaIncomingBinding
import io.zonarosa.messenger.databinding.V2ConversationItemMediaOutgoingBinding

/**
 * Pass-through interface for bridging incoming and outgoing media message views.
 *
 * Essentially, just a convenience wrapper since the layouts differ *very slightly* and
 * we want to be able to have each follow the same code-path.
 */
data class V2ConversationItemMediaBindingBridge(
  val textBridge: V2ConversationItemTextOnlyBindingBridge,
  val thumbnailStub: Stub<V2ConversationItemThumbnail>,
  val quoteStub: Stub<QuoteView>,
  val bodyContentSpacer: Space
)

/**
 * Wraps the binding in the bridge.
 */
fun V2ConversationItemMediaIncomingBinding.bridge(): V2ConversationItemMediaBindingBridge {
  val textBridge = V2ConversationItemTextOnlyBindingBridge(
    root = root,
    senderNameWithLabel = groupSenderNameWithLabel,
    senderPhoto = contactPhoto,
    senderBadge = badge,
    body = conversationItemBody,
    bodyWrapper = conversationItemBodyWrapper,
    reply = conversationItemReply,
    reactions = conversationItemReactions,
    deliveryStatus = null,
    footerDate = conversationItemFooterDate,
    footerExpiry = conversationItemExpirationTimer,
    footerBackground = conversationItemFooterBackground,
    alert = null,
    footerSpace = null,
    isIncoming = true,
    footerPinned = conversationItemFooterPinned
  )

  return V2ConversationItemMediaBindingBridge(
    textBridge = textBridge,
    thumbnailStub = Stub(conversationItemThumbnailStub),
    quoteStub = Stub(conversationItemQuoteStub),
    bodyContentSpacer = conversationItemContentSpacer
  )
}

/**
 * Wraps the binding in the bridge.
 */
fun V2ConversationItemMediaOutgoingBinding.bridge(): V2ConversationItemMediaBindingBridge {
  val textBridge = V2ConversationItemTextOnlyBindingBridge(
    root = root,
    senderNameWithLabel = null,
    senderPhoto = null,
    senderBadge = null,
    body = conversationItemBody,
    bodyWrapper = conversationItemBodyWrapper,
    reply = conversationItemReply,
    reactions = conversationItemReactions,
    deliveryStatus = conversationItemDeliveryStatus,
    footerDate = conversationItemFooterDate,
    footerExpiry = conversationItemExpirationTimer,
    footerBackground = conversationItemFooterBackground,
    alert = conversationItemAlert,
    footerSpace = footerEndPad,
    isIncoming = false,
    footerPinned = conversationItemFooterPinned
  )

  return V2ConversationItemMediaBindingBridge(
    textBridge = textBridge,
    thumbnailStub = Stub(conversationItemThumbnailStub),
    quoteStub = Stub(conversationItemQuoteStub),
    bodyContentSpacer = conversationItemContentSpacer
  )
}
