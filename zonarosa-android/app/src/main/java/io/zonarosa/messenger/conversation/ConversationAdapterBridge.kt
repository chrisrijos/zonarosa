/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation

import io.zonarosa.messenger.conversation.mutiselect.MultiselectPart

/**
 * Temporary shared interface between the two conversation adapters strictly for use in
 * shared decorators and other utils.
 */
interface ConversationAdapterBridge {
  companion object {
    const val PAYLOAD_TIMESTAMP = 0
    const val PAYLOAD_NAME_COLORS = 1
    const val PAYLOAD_SELECTED = 2
    const val PAYLOAD_PARENT_SCROLLING = 3
  }

  fun hasNoConversationMessages(): Boolean
  fun getConversationMessage(position: Int): ConversationMessage?
  fun consumePulseRequest(): PulseRequest?

  val selectedItems: Set<MultiselectPart>

  data class PulseRequest(val position: Int, val isOutgoing: Boolean)
}
