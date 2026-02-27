/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.migrations

import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.toPaymentMethodType
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import java.util.Currency

/**
 * Migrates all subscriber ids from the key value store into the database.
 */
internal class SubscriberIdMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(
  parameters
) {

  companion object {
    const val KEY = "SubscriberIdMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    Currency.getAvailableCurrencies().forEach { currency ->
      val subscriber = ZonaRosaStore.inAppPayments.getSubscriber(currency)

      if (subscriber != null) {
        ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(
          InAppPaymentSubscriberRecord(
            subscriberId = subscriber.subscriberId,
            currency = subscriber.currency,
            type = InAppPaymentSubscriberRecord.Type.DONATION,
            requiresCancel = ZonaRosaStore.inAppPayments.shouldCancelSubscriptionBeforeNextSubscribeAttempt,
            paymentMethodType = ZonaRosaStore.inAppPayments.getSubscriptionPaymentSourceType().toPaymentMethodType(),
            iapSubscriptionId = null
          )
        )
      }
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<SubscriberIdMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): SubscriberIdMigrationJob {
      return SubscriberIdMigrationJob(parameters)
    }
  }
}
