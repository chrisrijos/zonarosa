/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.billing

import io.zonarosa.core.util.billing.BillingApi
import io.zonarosa.core.util.billing.BillingDependencies

/**
 * Play billing factory. Returns empty implementation if message backups are not enabled.
 */
object BillingFactory {
  @JvmStatic
  fun create(billingDependencies: BillingDependencies, isBackupsAvailable: Boolean): BillingApi {
    return if (isBackupsAvailable) {
      BillingApiImpl(billingDependencies)
    } else {
      BillingApi.Empty
    }
  }
}
