/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.events

import io.zonarosa.messenger.recipients.Recipient
import java.util.concurrent.TimeUnit

/**
 * This is a data class to represent a reaction coming in over the wire in the format we need (mapped to a [Recipient]) in a way that can be easily
 * compared across Rx streams.
 */
data class GroupCallReactionEvent(val sender: Recipient, val reaction: String, val timestamp: Long) {
  fun getExpirationTimestamp(): Long {
    return timestamp + TimeUnit.SECONDS.toMillis(LIFESPAN_SECONDS)
  }

  companion object {
    const val LIFESPAN_SECONDS = 4L
  }
}
