/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.backup.DeletionState
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.components.settings.app.subscription.DonationSerializationHelper.toFiatValue
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.CoroutineJob
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.internal.push.SubscriptionsConfiguration
import java.math.BigDecimal
import java.util.Currency
import kotlin.concurrent.withLock

/**
 * Runs after registration to make sure we are on the backup level we expect on this device.
 */
class PostRegistrationBackupRedemptionJob : CoroutineJob {

  companion object {
    private val TAG = Log.tag(PostRegistrationBackupRedemptionJob::class)
    const val KEY = "PostRestoreBackupRedemptionJob"
  }

  constructor() : super(
    Parameters.Builder()
      .setQueue(InAppPaymentsRepository.getRecurringJobQueueKey(InAppPaymentType.RECURRING_BACKUP))
      .addConstraint(NetworkConstraint.KEY)
      .setMaxAttempts(Parameters.UNLIMITED)
      .setLifespan(Parameters.IMMORTAL)
      .build()
  )

  constructor(parameters: Parameters) : super(parameters)

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override suspend fun doRun(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      info("User is not registered. Exiting.")
      return Result.success()
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      info("Linked device. Exiting.")
      return Result.success()
    }

    if (ZonaRosaStore.backup.deletionState != DeletionState.NONE) {
      info("User is in the process of or has delete their backup. Exiting.")
      return Result.success()
    }

    if (ZonaRosaStore.backup.backupTier != MessageBackupTier.PAID) {
      info("Paid backups are not enabled on this device. Exiting.")
      return Result.success()
    }

    if (ZonaRosaStore.backup.backupTierInternalOverride != null) {
      info("User has internal override set for backup version. Exiting.")
      return Result.success()
    }

    if (ZonaRosaDatabase.inAppPayments.hasPendingBackupRedemption()) {
      info("User has a pending backup redemption. Retrying later.")
      return Result.retry(defaultBackoff())
    }

    val subscriber = InAppPaymentsRepository.getSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP)
    if (subscriber == null) {
      info("No subscriber information was available in the database. Exiting.")
      return Result.success()
    }

    info("Attempting to grab price information for records...")
    val subscription = RecurringInAppPaymentRepository.getActiveSubscriptionSync(InAppPaymentSubscriberRecord.Type.BACKUP).successOrNull()?.activeSubscription

    val emptyPrice = FiatMoney(BigDecimal.ZERO, ZonaRosaStore.inAppPayments.getOneTimeCurrency())
    val price: FiatMoney = if (subscription != null) {
      FiatMoney.fromZonaRosaNetworkAmount(subscription.amount, Currency.getInstance(subscription.currency))
    } else if (AppDependencies.billingApi.getApiAvailability().isSuccess) {
      AppDependencies.billingApi.queryProduct()?.price ?: emptyPrice
    } else {
      emptyPrice
    }

    if (price == emptyPrice) {
      warning("Could not resolve price, using empty price.")
    }

    InAppPaymentSubscriberRecord.Type.BACKUP.lock.withLock {
      if (ZonaRosaDatabase.inAppPayments.hasPendingBackupRedemption()) {
        warning("Backup is already pending redemption. Exiting.")
        return Result.success()
      }

      info("Creating a pending payment...")
      val id = ZonaRosaDatabase.inAppPayments.insert(
        type = InAppPaymentType.RECURRING_BACKUP,
        state = InAppPaymentTable.State.PENDING,
        subscriberId = InAppPaymentsRepository.requireSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP).subscriberId,
        endOfPeriod = null,
        inAppPaymentData = InAppPaymentData(
          badge = null,
          amount = price.toFiatValue(),
          level = SubscriptionsConfiguration.BACKUPS_LEVEL.toLong(),
          recipientId = Recipient.self().id.serialize(),
          paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
          redemption = InAppPaymentData.RedemptionState(
            stage = InAppPaymentData.RedemptionState.Stage.INIT
          )
        )
      )

      info("Submitting job chain.")
      InAppPaymentPurchaseTokenJob.createJobChain(
        inAppPayment = ZonaRosaDatabase.inAppPayments.getById(id)!!
      ).enqueue()
    }

    return Result.success()
  }

  override fun onFailure() = Unit

  private fun info(message: String, throwable: Throwable? = null) {
    Log.i(TAG, message, throwable, true)
  }

  private fun warning(message: String, throwable: Throwable? = null) {
    Log.w(TAG, message, throwable, true)
  }

  class Factory : Job.Factory<PostRegistrationBackupRedemptionJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): PostRegistrationBackupRedemptionJob {
      return PostRegistrationBackupRedemptionJob(parameters)
    }
  }
}
