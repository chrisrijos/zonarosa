/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.events

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class GroupCallRaiseHandEvent(val sender: CallParticipant, private val timestampMillis: Long) {

  val timestamp = timestampMillis.milliseconds

  fun getCollapseTimestamp(): Duration {
    return timestamp + LIFESPAN
  }

  companion object {
    private val LIFESPAN = 4L.seconds
  }
}
