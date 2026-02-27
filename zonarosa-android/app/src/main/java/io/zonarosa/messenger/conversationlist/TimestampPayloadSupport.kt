/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversationlist

/**
 * Generic interface for the adapters to support updating the
 * timestamp in a given row as opposed to rebinding every item.
 */
interface TimestampPayloadSupport {
  fun notifyTimestampPayloadUpdate()
}
