/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.billing

import io.zonarosa.core.util.money.FiatMoney

/**
 * Represents a purchasable product from the Google Play Billing API
 */
data class BillingProduct(
  val price: FiatMoney
)
