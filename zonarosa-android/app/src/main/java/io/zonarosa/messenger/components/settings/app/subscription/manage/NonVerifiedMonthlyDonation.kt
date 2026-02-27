/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.manage

import io.zonarosa.core.util.money.FiatMoney

/**
 * Represents a monthly donation via iDEAL that is still pending user verification in
 * their 3rd party app.
 */
data class NonVerifiedMonthlyDonation(
  val timestamp: Long,
  val price: FiatMoney,
  val level: Int,
  val checkedVerification: Boolean
)
