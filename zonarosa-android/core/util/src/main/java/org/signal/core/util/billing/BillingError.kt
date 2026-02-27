/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.billing

class BillingError(
  val billingResponseCode: Int
) : Exception("$billingResponseCode")
