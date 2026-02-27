/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.events

import io.zonarosa.ringrtc.GroupCall.SpeechEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class GroupCallSpeechEvent @JvmOverloads constructor(
  val speechEvent: SpeechEvent,
  private val timestampMs: Long = System.currentTimeMillis()
) {
  fun getCollapseTimestamp(): Duration {
    return timestampMs.milliseconds + LIFESPAN
  }

  companion object {
    private val LIFESPAN = 4L.seconds
  }
}
