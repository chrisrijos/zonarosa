/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2

/**
 * State information used to display the scroll to next mention and scroll to bottom buttons.
 */
data class ConversationScrollButtonState(
  val hideScrollButtonsForReactionOverlay: Boolean = false,
  val showScrollButtonsForScrollPosition: Boolean = false,
  val willScrollToBottomOnNewMessage: Boolean = true,
  val unreadCount: Int = 0,
  val hasMentions: Boolean = false
) {
  val showScrollButtons: Boolean
    get() = !hideScrollButtonsForReactionOverlay && (showScrollButtonsForScrollPosition || (!willScrollToBottomOnNewMessage && unreadCount > 0))
}
