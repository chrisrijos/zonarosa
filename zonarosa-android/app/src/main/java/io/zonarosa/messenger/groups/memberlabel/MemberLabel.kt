/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.groups.memberlabel

import androidx.annotation.ColorInt

/**
 * A member's custom label within a group.
 */
data class MemberLabel(
  val emoji: String?,
  val text: String
)

data class StyledMemberLabel(
  val label: MemberLabel,
  @param:ColorInt val tintColor: Int
)
