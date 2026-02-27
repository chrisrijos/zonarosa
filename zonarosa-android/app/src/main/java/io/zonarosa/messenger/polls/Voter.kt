/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.polls

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Class to track someone who has voted in an option within a poll.
 */
@Parcelize
data class Voter(
  val id: Long,
  val voteCount: Int
) : Parcelable
