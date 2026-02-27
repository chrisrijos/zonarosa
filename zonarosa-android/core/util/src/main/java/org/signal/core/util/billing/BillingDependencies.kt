/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.billing

import android.content.Context

/**
 * Provides a dependency model by which the billing api can request different resources.
 */
interface BillingDependencies {
  /**
   * Application context
   */
  val context: Context

  /**
   * Get the product id from the donations configuration object.
   */
  suspend fun getProductId(): String

  /**
   * Get the base plan id from the donations configuration object.
   */
  suspend fun getBasePlanId(): String
}
