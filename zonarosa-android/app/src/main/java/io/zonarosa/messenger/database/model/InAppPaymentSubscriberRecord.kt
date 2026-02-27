/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.model

import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.service.api.storage.IAPSubscriptionId
import io.zonarosa.service.api.subscriptions.SubscriberId
import java.util.Currency
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Represents a SubscriberId and metadata that can be used for a recurring
 * subscription of the given type. Stored in InAppPaymentSubscriberTable
 */
data class InAppPaymentSubscriberRecord(
  val subscriberId: SubscriberId,
  val type: Type,
  val requiresCancel: Boolean,
  val paymentMethodType: InAppPaymentData.PaymentMethodType,
  val currency: Currency?,
  val iapSubscriptionId: IAPSubscriptionId?
) {
  /**
   * Serves as the mutex by which to perform mutations to subscriptions.
   */
  enum class Type(val code: Int, val jobQueue: String, val inAppPaymentType: InAppPaymentType, val lock: Lock = ReentrantLock()) {
    /**
     * A recurring donation
     */
    DONATION(0, "recurring-donations", InAppPaymentType.RECURRING_DONATION),

    /**
     * A recurring backups subscription
     */
    BACKUP(1, "recurring-backups", InAppPaymentType.RECURRING_BACKUP)
  }
}
