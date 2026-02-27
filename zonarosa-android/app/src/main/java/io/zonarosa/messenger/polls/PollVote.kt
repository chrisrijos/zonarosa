/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.polls

import io.zonarosa.messenger.recipients.RecipientId

/**
 * Tracks general information of a poll vote including who they are and what poll they voted in. Primarily used in notifications.
 */
data class PollVote(
  val pollId: Long,
  val voterId: RecipientId,
  val question: String,
  val dateReceived: Long = 0
)
