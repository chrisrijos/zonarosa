/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.computed

data class FormattedDate(
  val isRelative: Boolean,
  val isNow: Boolean,
  val value: String,
  val contentDescValue: String
)
