/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.dependencies

import android.content.Context
import io.zonarosa.core.util.billing.BillingDependencies
import io.zonarosa.core.util.billing.BillingError
import io.zonarosa.service.internal.push.SubscriptionsConfiguration
import java.util.Locale

/**
 * Dependency object for Google Play Billing.
 */
object GooglePlayBillingDependencies : BillingDependencies {

  private const val BILLING_PRODUCT_ID_NOT_AVAILABLE = -1000

  override val context: Context get() = AppDependencies.application

  override suspend fun getProductId(): String {
    val config = AppDependencies.donationsService.getDonationsConfiguration(Locale.getDefault())

    if (config.result.isPresent) {
      return config.result.get().backupConfiguration.backupLevelConfigurationMap[SubscriptionsConfiguration.BACKUPS_LEVEL]?.playProductId ?: throw BillingError(BILLING_PRODUCT_ID_NOT_AVAILABLE)
    } else {
      throw BillingError(BILLING_PRODUCT_ID_NOT_AVAILABLE)
    }
  }

  override suspend fun getBasePlanId(): String {
    return "monthly"
  }
}
