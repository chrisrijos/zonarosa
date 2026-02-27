package io.zonarosa.messenger.components.settings.app.subscription

import androidx.annotation.CheckResult
import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.requireSubscriberType
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.toPaymentSourceType
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository.cancelActiveSubscriptionIfNecessarySync
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository.cancelActiveSubscriptionSync
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository.getActiveSubscriptionSync
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository.rotateSubscriberIdSync
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.InAppPaymentKeepAliveJob
import io.zonarosa.messenger.jobs.InAppPaymentRecurringContextJob
import io.zonarosa.messenger.jobs.MultiDeviceSubscriptionSyncRequestJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.subscription.LevelUpdate
import io.zonarosa.messenger.subscription.LevelUpdateOperation
import io.zonarosa.messenger.subscription.Subscription
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.storage.IAPSubscriptionId
import io.zonarosa.service.api.subscriptions.ActiveSubscription
import io.zonarosa.service.api.subscriptions.IdempotencyKey
import io.zonarosa.service.api.subscriptions.SubscriberId
import io.zonarosa.service.internal.EmptyResponse
import io.zonarosa.service.internal.ServiceResponse
import io.zonarosa.service.internal.push.SubscriptionsConfiguration
import java.math.BigDecimal
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shared methods for operating on recurring subscriptions, shared between donations and backups.
 */
object RecurringInAppPaymentRepository {

  private val TAG = Log.tag(RecurringInAppPaymentRepository::class.java)

  private val donationsService = AppDependencies.donationsService

  /**
   * Passthrough Rx wrapper for [getActiveSubscriptionSync] dispatching on io thread-pool.
   */
  @CheckResult
  fun getActiveSubscription(type: InAppPaymentSubscriberRecord.Type): Single<ActiveSubscription> {
    return Single.fromCallable {
      getActiveSubscriptionSync(type).successOrThrow()
    }.subscribeOn(Schedulers.io())
  }

  /** A fake paid subscription to return when the backup tier override is set. */
  private val MOCK_PAID_SUBSCRIPTION = ActiveSubscription(
    ActiveSubscription.Subscription(
      SubscriptionsConfiguration.BACKUPS_LEVEL,
      "USD",
      BigDecimal(42),
      2147472000,
      true,
      2147472000,
      false,
      "active",
      "USA",
      "credit-card",
      false
    ),
    null
  )

  /**
   * Gets the active subscription if it exists for the given [InAppPaymentSubscriberRecord.Type]
   */
  @WorkerThread
  fun getActiveSubscriptionSync(type: InAppPaymentSubscriberRecord.Type): NetworkResult<ActiveSubscription> {
    if (type == InAppPaymentSubscriberRecord.Type.BACKUP && ZonaRosaStore.backup.backupTierInternalOverride == MessageBackupTier.PAID) {
      Log.d(TAG, "Returning mock paid subscription.")
      return NetworkResult.Success(MOCK_PAID_SUBSCRIPTION)
    }

    val response = InAppPaymentsRepository.getSubscriber(type)?.let {
      donationsService.getSubscription(it.subscriberId)
    } ?: return NetworkResult.Success(ActiveSubscription.EMPTY)

    response.result.ifPresent { result ->
      if (result.isActive && result.activeSubscription.endOfCurrentPeriod > ZonaRosaStore.inAppPayments.getLastEndOfPeriod()) {
        InAppPaymentKeepAliveJob.enqueueAndTrackTime(System.currentTimeMillis().milliseconds)
      }
    }

    return response.toNetworkResult()
  }

  /**
   * Gets a list of subscriptions available via the donations configuration.
   */
  @CheckResult
  fun getSubscriptions(): Single<List<Subscription>> {
    return Single
      .fromCallable { donationsService.getDonationsConfiguration(Locale.getDefault()) }
      .subscribeOn(Schedulers.io())
      .flatMap { it.flattenResult() }
      .map { config ->
        config.getSubscriptionLevels().map { (level, levelConfig) ->
          Subscription(
            id = level.toString(),
            level = level,
            badge = Badges.fromServiceBadge(levelConfig.badge),
            prices = config.getSubscriptionAmounts(level)
          )
        }
      }
  }

  /**
   * Syncs the user account record, dispatches on the io thread-pool
   */
  @CheckResult
  fun syncAccountRecord(): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }.subscribeOn(Schedulers.io())
  }

  /**
   * Since PayPal and Stripe can't interoperate, we need to be able to rotate the subscriber ID
   * in case of failures.
   */
  @WorkerThread
  fun rotateSubscriberIdSync(subscriberType: InAppPaymentSubscriberRecord.Type) {
    Log.d(TAG, "Rotating SubscriberId due to alternate payment processor...", true)
    if (InAppPaymentsRepository.getSubscriber(subscriberType) != null) {
      cancelActiveSubscriptionSync(subscriberType)
      updateLocalSubscriptionStateAndScheduleDataSync(subscriberType)
    }

    ensureSubscriberIdSync(subscriberType, isRotation = true)
  }

  /**
   * Passthrough Rx wrapper for [rotateSubscriberIdSync] dispatching on io thread-pool.
   */
  @CheckResult
  fun rotateSubscriberId(subscriberType: InAppPaymentSubscriberRecord.Type): Completable {
    return Completable.fromAction {
      rotateSubscriberIdSync(subscriberType)
    }.subscribeOn(Schedulers.io())
  }

  /**
   * Ensures that the given [InAppPaymentSubscriberRecord.Type] has a [SubscriberId] that has been sent to the ZonaRosa Service.
   * Will also record and synchronize this data with storage sync.
   */
  @WorkerThread
  fun ensureSubscriberIdSync(subscriberType: InAppPaymentSubscriberRecord.Type, isRotation: Boolean = false, iapSubscriptionId: IAPSubscriptionId? = null) {
    Log.d(TAG, "Ensuring SubscriberId for type $subscriberType exists on ZonaRosa service {isRotation?$isRotation}...", true)

    val subscriberId = if (isRotation) {
      SubscriberId.generate()
    } else {
      InAppPaymentsRepository.getSubscriber(subscriberType)?.subscriberId ?: SubscriberId.generate()
    }

    donationsService.putSubscription(subscriberId).resultOrThrow

    Log.d(TAG, "Successfully set SubscriberId exists on ZonaRosa service.", true)

    InAppPaymentsRepository.setSubscriber(
      InAppPaymentSubscriberRecord(
        subscriberId = subscriberId,
        currency = if (subscriberType == InAppPaymentSubscriberRecord.Type.DONATION) {
          ZonaRosaStore.inAppPayments.getRecurringDonationCurrency()
        } else {
          null
        },
        type = subscriberType,
        requiresCancel = false,
        paymentMethodType = if (subscriberType == InAppPaymentSubscriberRecord.Type.BACKUP) {
          InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING
        } else {
          InAppPaymentData.PaymentMethodType.UNKNOWN
        },
        iapSubscriptionId = iapSubscriptionId
      )
    )

    ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  /**
   * Cancels the active subscription via the ZonaRosa service.
   */
  @WorkerThread
  fun cancelActiveSubscriptionSync(subscriberType: InAppPaymentSubscriberRecord.Type) {
    Log.d(TAG, "Canceling active subscription...", true)
    val localSubscriber = InAppPaymentsRepository.requireSubscriber(subscriberType)

    val serviceResponse: ServiceResponse<EmptyResponse> = donationsService.cancelSubscription(localSubscriber.subscriberId)
    serviceResponse.resultOrThrow

    Log.d(TAG, "Cancelled active subscription.", true)
    ZonaRosaStore.inAppPayments.updateLocalStateForManualCancellation(subscriberType)
    MultiDeviceSubscriptionSyncRequestJob.enqueue()
    InAppPaymentsRepository.scheduleSyncForAccountRecordChange()
  }

  /**
   * Passthrough Rx wrapper for [cancelActiveSubscriptionSync] dispatching on io thread-pool.
   */
  @CheckResult
  fun cancelActiveSubscription(subscriberType: InAppPaymentSubscriberRecord.Type): Completable {
    return Completable
      .fromAction { cancelActiveSubscriptionSync(subscriberType) }
      .subscribeOn(Schedulers.io())
  }

  /**
   * If the subscriber of the given type has been marked as "requires cancel", this method will perform the cancellation and
   * sync the appropriate data.
   */
  @WorkerThread
  fun cancelActiveSubscriptionIfNecessarySync(subscriberType: InAppPaymentSubscriberRecord.Type) {
    val shouldCancel = InAppPaymentsRepository.getShouldCancelSubscriptionBeforeNextSubscribeAttempt(subscriberType)
    if (shouldCancel) {
      cancelActiveSubscriptionSync(subscriberType)
      ZonaRosaStore.inAppPayments.updateLocalStateForManualCancellation(subscriberType)
      MultiDeviceSubscriptionSyncRequestJob.enqueue()
    }
  }

  /**
   * Passthrough Rx wrapper for [cancelActiveSubscriptionIfNecessarySync] dispatching on io thread-pool.
   */
  @CheckResult
  fun cancelActiveSubscriptionIfNecessary(subscriberType: InAppPaymentSubscriberRecord.Type): Completable {
    return Completable.fromAction {
      cancelActiveSubscriptionIfNecessarySync(subscriberType)
    }.subscribeOn(Schedulers.io())
  }

  /**
   * Passthrough Rx wrapper for [InAppPaymentsRepository.getLatestPaymentMethodType] dispatching on io thread-pool.
   */
  @CheckResult
  fun getPaymentSourceTypeOfLatestSubscription(subscriberType: InAppPaymentSubscriberRecord.Type): Single<PaymentSourceType> {
    return Single.fromCallable {
      InAppPaymentsRepository.getLatestPaymentMethodType(subscriberType).toPaymentSourceType()
    }.subscribeOn(Schedulers.io())
  }

  /**
   * Sets the subscription level as per the data in the InAppPayment.
   *
   * This method mutates the [InAppPaymentTable.InAppPayment] and thus returns a new instance.
   */
  @CheckResult
  @WorkerThread
  fun setSubscriptionLevelSync(inAppPayment: InAppPaymentTable.InAppPayment): InAppPaymentTable.InAppPayment {
    val subscriptionLevel = inAppPayment.data.level.toString()
    val subscriberType = inAppPayment.type.requireSubscriberType()
    val subscriber = InAppPaymentsRepository.requireSubscriber(subscriberType)

    getOrCreateLevelUpdateOperation(TAG, subscriptionLevel).use { operation ->
      ZonaRosaDatabase.inAppPayments.update(
        inAppPayment = inAppPayment.copy(
          subscriberId = subscriber.subscriberId,
          data = inAppPayment.data.newBuilder().redemption(
            redemption = InAppPaymentData.RedemptionState(
              stage = InAppPaymentData.RedemptionState.Stage.INIT
            )
          ).build()
        )
      )

      Log.d(TAG, "Attempting to set user subscription level to $subscriptionLevel", true)

      val response = AppDependencies.donationsService.updateSubscriptionLevel(
        subscriber.subscriberId,
        subscriptionLevel,
        subscriber.currency!!.currencyCode,
        operation.idempotencyKey.serialize(),
        subscriberType.lock
      )

      if (response.status == 200 || response.status == 204) {
        Log.d(TAG, "Successfully set user subscription to level $subscriptionLevel with response code ${response.status}", true)
        ZonaRosaStore.inAppPayments.updateLocalStateForLocalSubscribe(subscriberType)
        syncAccountRecord().subscribe()
      } else {
        if (response.applicationError.isPresent) {
          Log.w(TAG, "Failed to set user subscription to level $subscriptionLevel with response code ${response.status}", response.applicationError.get(), true)
          ZonaRosaStore.inAppPayments.clearLevelOperations()
        } else {
          Log.w(TAG, "Failed to set user subscription to level $subscriptionLevel", response.executionError.orElse(null), true)
        }

        response.resultOrThrow
        error("Should never get here.")
      }
    }

    Log.d(TAG, "Enqueuing request response job chain.", true)
    val freshPayment = ZonaRosaDatabase.inAppPayments.getById(inAppPayment.id)!!
    InAppPaymentRecurringContextJob.createJobChain(freshPayment).enqueue()

    return freshPayment
  }

  /**
   * Get or create a [LevelUpdateOperation]
   *
   * This allows us to ensure the same idempotency key is used across multiple attempts for the same level.
   */
  fun getOrCreateLevelUpdateOperation(tag: String, subscriptionLevel: String): LevelUpdateOperation {
    Log.d(tag, "Retrieving level update operation for $subscriptionLevel")
    val levelUpdateOperation = ZonaRosaStore.inAppPayments.getLevelOperation(subscriptionLevel)
    return if (levelUpdateOperation == null) {
      val newOperation = LevelUpdateOperation(
        idempotencyKey = IdempotencyKey.generate(),
        level = subscriptionLevel
      )

      ZonaRosaStore.inAppPayments.setLevelOperation(newOperation)
      LevelUpdate.updateProcessingState(true)
      Log.d(tag, "Created a new operation for $subscriptionLevel")
      newOperation
    } else {
      LevelUpdate.updateProcessingState(true)
      Log.d(tag, "Reusing operation for $subscriptionLevel")
      levelUpdateOperation
    }
  }

  /**
   * Update local state information and schedule a storage sync for the change. This method
   * assumes you've already properly called the DELETE method for the stored ID on the server.
   */
  @WorkerThread
  private fun updateLocalSubscriptionStateAndScheduleDataSync(subscriberType: InAppPaymentSubscriberRecord.Type) {
    Log.d(TAG, "Marking subscription cancelled...", true)
    ZonaRosaStore.inAppPayments.updateLocalStateForManualCancellation(subscriberType)
    MultiDeviceSubscriptionSyncRequestJob.enqueue()
    ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()
  }
}
