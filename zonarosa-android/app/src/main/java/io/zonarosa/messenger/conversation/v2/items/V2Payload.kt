/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.items

/**
 * Describes the different V2-Specific payloads that can be emitted.
 */
enum class V2Payload {
  SEARCH_QUERY_UPDATED,
  PLAY_INLINE_CONTENT,
  WALLPAPER,
  MESSAGE_REQUEST_STATE
}
