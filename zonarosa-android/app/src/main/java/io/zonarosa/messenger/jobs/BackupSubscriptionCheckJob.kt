/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import androidx.annotation.VisibleForTesting
import io.zonarosa.core.util.billing.BillingProduct
import io.zonarosa.core.util.billing.BillingPurchaseResult
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.backup.DeletionState
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.components.settings.app.backups.BackupStateObserver
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
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.storage.IAPSubscriptionId
import io.zonarosa.service.api.subscriptions.ActiveSubscription
import io.zonarosa.service.internal.push.SubscriptionsConfiguration
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

/**
 * Checks and rectifies state pertaining to backups subscriptions.
 */
class BackupSubscriptionCheckJob private constructor(parameters: Parameters) : CoroutineJob(parameters) {

  companion object {
    private val TAG = Log.tag(BackupSubscriptionCheckJob::class)

    const val KEY = "BackupSubscriptionCheckJob"

    @VisibleForTesting
    fun create(): BackupSubscriptionCheckJob {
      return BackupSubscriptionCheckJob(
        Parameters.Builder()
          .setQueue(InAppPaymentsRepository.getRecurringJobQueueKey(InAppPaymentType.RECURRING_BACKUP))
          .addConstraint(NetworkConstraint.KEY)
          .setMaxAttempts(Parameters.UNLIMITED)
          .setMaxInstancesForFactory(1)
          .build()
      )
    }

    @JvmStatic
    fun enqueueIfAble() {
      val job = create()

      AppDependencies.jobManager.add(job)
    }
  }

  override suspend fun doRun(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.i(TAG, "User is not registered. Clearing mismatch value and exiting.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Linked device. Clearing mismatch value and exiting.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (!AppDependencies.billingApi.getApiAvailability().isSuccess) {
      Log.i(TAG, "Google Play Billing API is not available on this device. Clearing mismatch value and exiting.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (ZonaRosaStore.backup.deletionState != DeletionState.NONE) {
      Log.i(TAG, "User is in the process of or has delete their backup. Clearing mismatch value and exiting.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (!ZonaRosaStore.backup.areBackupsEnabled) {
      Log.i(TAG, "Backups are not enabled on this device. Clearing mismatch value and exiting.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (ZonaRosaStore.backup.backupTierInternalOverride != null) {
      Log.i(TAG, "User has internal override set for backup version. Clearing mismatch value and exiting.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (ZonaRosaDatabase.inAppPayments.hasPrePendingRecurringTransaction(InAppPaymentType.RECURRING_BACKUP)) {
      Log.i(TAG, "A backup redemption is in the pre-pending state. Clearing mismatch and skipping check job.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    if (ZonaRosaDatabase.inAppPayments.hasPendingBackupRedemption()) {
      Log.i(TAG, "A backup redemption is pending. Clearing mismatch and skipping check job.", true)
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    val purchase: BillingPurchaseResult = AppDependencies.billingApi.queryPurchases()
    Log.i(TAG, "Retrieved purchase result from Billing api: $purchase", true)

    if (purchase !is BillingPurchaseResult.Success && purchase !is BillingPurchaseResult.None) {
      Log.w(TAG, "Possible error when grabbing purchase from billing API. Clearing mismatch and exiting.")
      ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
      return Result.success()
    }

    val hasActivePurchase = purchase is BillingPurchaseResult.Success && purchase.isAcknowledged
    val product: BillingProduct? = AppDependencies.billingApi.queryProduct()

    if (product == null) {
      Log.w(TAG, "Google Play Billing product not available. Exiting.", true)
      return Result.failure()
    }

    InAppPaymentSubscriberRecord.Type.BACKUP.lock.withLock {
      val inAppPayment = ZonaRosaDatabase.inAppPayments.getLatestInAppPaymentByType(InAppPaymentType.RECURRING_BACKUP)

      if (inAppPayment?.state == InAppPaymentTable.State.PENDING) {
        Log.i(TAG, "User has a pending in-app payment. Clearing mismatch value and re-checking later.", true)
        ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
        return Result.success()
      }

      val activeSubscriptionResult = RecurringInAppPaymentRepository.getActiveSubscriptionSync(InAppPaymentSubscriberRecord.Type.BACKUP)
      val activeSubscription: ActiveSubscription? = when (activeSubscriptionResult) {
        is NetworkResult.ApplicationError<ActiveSubscription>, is NetworkResult.NetworkError<ActiveSubscription> -> {
          Log.w(TAG, "Encountered an app-level or network-level error. Failing.", activeSubscriptionResult.getCause(), true)
          return Result.failure()
        }
        is NetworkResult.StatusCodeError<ActiveSubscription> -> {
          Log.w(TAG, "Encountered a status-code error.", activeSubscriptionResult.getCause(), true)
          null
        }
        is NetworkResult.Success<ActiveSubscription> -> {
          Log.i(TAG, "Successfully retrieved the user's active subscription object.", true)
          activeSubscriptionResult.result
        }
      }

      val hasActiveZonaRosaSubscription = activeSubscription?.isActive == true

      checkForFailedOrCanceledSubscriptionState(activeSubscription)

      val isZonaRosaSubscriptionFailedOrCanceled = activeSubscription?.willCancelAtPeriodEnd() == true
      if (hasActiveZonaRosaSubscription && !isZonaRosaSubscriptionFailedOrCanceled) {
        checkAndSynchronizeZkCredentialTierWithStoredLocalTier()
      }

      val hasActivePaidBackupTier = ZonaRosaStore.backup.backupTier == MessageBackupTier.PAID
      val hasValidActiveState = hasActivePaidBackupTier && hasActiveZonaRosaSubscription && hasActivePurchase
      val hasValidInactiveState = !hasActivePaidBackupTier && !hasActiveZonaRosaSubscription && !hasActivePurchase

      val purchaseToken = if (hasActivePurchase) {
        purchase.purchaseToken
      } else {
        null
      }

      val hasTokenMismatch = purchaseToken?.let { hasLocalDevicePurchaseTokenMismatch(purchaseToken) } == true
      if (hasActiveZonaRosaSubscription && hasTokenMismatch) {
        Log.i(TAG, "Encountered token mismatch with an active ZonaRosa subscription. Attempting to redeem against latest token.", true)
        rotateAndRedeem(purchaseToken, product.price)
        ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
        return Result.success()
      } else if (purchaseToken != null && hasActiveZonaRosaSubscription && !hasActivePaidBackupTier && !ZonaRosaDatabase.inAppPayments.hasPendingBackupRedemption()) {
        Log.i(TAG, "We have an active zonarosa subscription and active purchase, but no entitlement and no pending redemption. Enqueuing a redemption now.")
        rotateAndRedeem(purchaseToken, product.price)
        ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
        return Result.success()
      } else {
        if (hasValidActiveState || hasValidInactiveState) {
          Log.i(TAG, "Valid state: (hasValidActiveState: $hasValidActiveState, hasValidInactiveState: $hasValidInactiveState). Clearing mismatch value and exiting.", true)
          ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
          return Result.success()
        } else {
          val isGooglePlayBillingCanceled = purchase is BillingPurchaseResult.Success && !purchase.isAutoRenewing

          if (isGooglePlayBillingCanceled && (!hasActiveZonaRosaSubscription || isZonaRosaSubscriptionFailedOrCanceled)) {
            Log.i(
              TAG,
              "Valid cancel state. Clearing mismatch. (isGooglePlayBillingCanceled: true, hasActiveZonaRosaSubscription: $hasActiveZonaRosaSubscription, isZonaRosaSubscriptionFailedOrCanceled: $isZonaRosaSubscriptionFailedOrCanceled",
              true
            )
            ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
            return Result.success()
          } else if (hasActivePurchase && !hasActiveZonaRosaSubscription && ZonaRosaStore.backup.backupTier == MessageBackupTier.FREE) {
            Log.i(TAG, "Mismatched state but user has no ZonaRosa Service subscription and is on the free tier. Clearing flag.", true)

            ZonaRosaStore.backup.subscriptionStateMismatchDetected = false
            return Result.success()
          } else {
            Log.w(TAG, "State mismatch: (hasActivePaidBackupTier: $hasActivePaidBackupTier, hasActiveZonaRosaSubscription: $hasActiveZonaRosaSubscription, hasActivePurchase: $hasActivePurchase). Setting mismatch value and exiting.", true)

            ZonaRosaStore.backup.subscriptionStateMismatchDetected = true
            return Result.success()
          }
        }
      }
    }
  }

  /**
   * If we detect that we have an active subscription, we want to check to make sure our ZK credentials are good. If they aren't, we should clear them.
   * This will also synchronize our backup tier value with whatever the refreshed Zk tier thinks we are on, if necessary.
   */
  private fun checkAndSynchronizeZkCredentialTierWithStoredLocalTier() {
    Log.i(TAG, "Detected an active, non-failed, non-canceled zonarosa subscription. Synchronizing backup tier with value from server.", true)

    val zkTier: MessageBackupTier? = when (val result = BackupRepository.getBackupTierWithoutDowngrade()) {
      is NetworkResult.Success -> result.result
      else -> null
    }

    if (zkTier == ZonaRosaStore.backup.backupTier) {
      Log.i(TAG, "ZK credential tier is in sync with our stored backup tier.", true)
    } else {
      Log.w(TAG, "ZK credential tier is not in sync with our stored backup tier, flushing credentials and retrying.", true)
      BackupRepository.resetInitializedStateAndAuthCredentials()

      BackupRepository.getBackupTier().runIfSuccessful {
        Log.i(TAG, "Refreshed credentials. Synchronizing stored backup tier with ZK result.")
        ZonaRosaStore.backup.backupTier = it
      }
    }
  }

  /**
   * Checks for a payment failure / subscription cancellation. If either of these things occur, we will mark when to display
   * the "download your data" notifier sheet.
   */
  private fun checkForFailedOrCanceledSubscriptionState(activeSubscription: ActiveSubscription?) {
    if (activeSubscription?.willCancelAtPeriodEnd() == true && activeSubscription.activeSubscription != null) {
      Log.i(TAG, "Subscription either has a payment failure or has been canceled.")

      val response = ZonaRosaNetwork.account.whoAmI()
      response.runIfSuccessful { whoAmI ->
        val backupExpiration = whoAmI.entitlements?.backup?.expirationSeconds?.seconds
        if (backupExpiration != null) {
          Log.i(TAG, "Marking subscription failed or canceled.")
          ZonaRosaStore.backup.setDownloadNotifierToTriggerAtHalfwayPoint(backupExpiration)
          InAppPaymentsRepository.updateBackupInAppPaymentWithCancelation(activeSubscription)
          BackupStateObserver.notifyBackupStateChanged()
        } else {
          Log.w(TAG, "Failed to mark, no entitlement was found on WhoAmIResponse")
        }
      }

      if (response.getCause() != null) {
        Log.w(TAG, "Failed to get WhoAmI from service.", response.getCause())
      }
    } else if (activeSubscription != null) {
      InAppPaymentsRepository.clearCancelation(activeSubscription)
    }
  }

  private fun rotateAndRedeem(localDevicePurchaseToken: String, localProductPrice: FiatMoney) {
    RecurringInAppPaymentRepository.ensureSubscriberIdSync(
      subscriberType = InAppPaymentSubscriberRecord.Type.BACKUP,
      isRotation = true,
      iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken(localDevicePurchaseToken)
    )

    ZonaRosaDatabase.inAppPayments.clearCreated()

    val id = ZonaRosaDatabase.inAppPayments.insert(
      type = InAppPaymentType.RECURRING_BACKUP,
      state = InAppPaymentTable.State.PENDING,
      subscriberId = InAppPaymentsRepository.requireSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP).subscriberId,
      endOfPeriod = null,
      inAppPaymentData = InAppPaymentData(
        badge = null,
        amount = localProductPrice.toFiatValue(),
        level = SubscriptionsConfiguration.BACKUPS_LEVEL.toLong(),
        recipientId = Recipient.self().id.serialize(),
        paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
        redemption = InAppPaymentData.RedemptionState(
          stage = InAppPaymentData.RedemptionState.Stage.INIT
        )
      )
    )

    InAppPaymentPurchaseTokenJob.createJobChain(
      inAppPayment = ZonaRosaDatabase.inAppPayments.getById(id)!!
    ).enqueue()
  }

  private fun hasLocalDevicePurchaseTokenMismatch(localDevicePurchaseToken: String): Boolean {
    val subscriber = InAppPaymentsRepository.getSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP)

    return subscriber?.iapSubscriptionId?.purchaseToken != localDevicePurchaseToken
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  class Factory : Job.Factory<BackupSubscriptionCheckJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): BackupSubscriptionCheckJob {
      return BackupSubscriptionCheckJob(parameters)
    }
  }
}
