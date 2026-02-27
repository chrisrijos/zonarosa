package io.zonarosa.messenger.components.settings.app.subscription.donate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.subjects.PublishSubject
import io.zonarosa.core.util.BidiUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.core.util.money.PlatformCurrencyUtil
import io.zonarosa.core.util.orNull
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.components.settings.app.subscription.DonationSerializationHelper.toFiatValue
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.components.settings.app.subscription.OneTimeInAppPaymentRepository
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository
import io.zonarosa.messenger.components.settings.app.subscription.boost.Boost
import io.zonarosa.messenger.components.settings.app.subscription.manage.DonationRedemptionJobStatus
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.database.model.databaseprotos.PendingOneTimeDonation
import io.zonarosa.messenger.database.model.isExpired
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.subscription.LevelUpdate
import io.zonarosa.messenger.subscription.Subscription
import io.zonarosa.messenger.util.InternetConnectionObserver
import io.zonarosa.messenger.util.rx.RxStore
import io.zonarosa.service.api.subscriptions.ActiveSubscription
import io.zonarosa.service.api.subscriptions.SubscriberId
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Optional

/**
 * Contains the logic to manage the UI of the unified donations screen.
 * Does not directly deal with performing payments, this ViewModel is
 * only in charge of rendering our "current view of the world."
 */
class DonateToZonaRosaViewModel(
  startType: InAppPaymentType
) : ViewModel() {

  companion object {
    private val TAG = Log.tag(DonateToZonaRosaViewModel::class.java)
  }

  private val store = RxStore(DonateToZonaRosaState(inAppPaymentType = startType))
  private val oneTimeDonationDisposables = CompositeDisposable()
  private val monthlyDonationDisposables = CompositeDisposable()
  private val networkDisposable = CompositeDisposable()
  private val actionDisposable = CompositeDisposable()
  private val _actions = PublishSubject.create<DonateToZonaRosaAction>()
  private val _activeSubscription = PublishSubject.create<ActiveSubscription>()
  private val _inAppPaymentId = BehaviorProcessor.create<InAppPaymentTable.InAppPaymentId>()

  val state = store.stateFlowable.observeOn(AndroidSchedulers.mainThread())
  val actions: Observable<DonateToZonaRosaAction> = _actions.observeOn(AndroidSchedulers.mainThread())
  val inAppPaymentId: Flowable<InAppPaymentTable.InAppPaymentId> = _inAppPaymentId.onBackpressureLatest().distinctUntilChanged()

  init {
    initializeOneTimeDonationState(OneTimeInAppPaymentRepository)
    initializeMonthlyDonationState(RecurringInAppPaymentRepository)

    networkDisposable += InternetConnectionObserver
      .observe()
      .distinctUntilChanged()
      .subscribe { isConnected ->
        if (isConnected) {
          retryMonthlyDonationState()
          retryOneTimeDonationState()
        }
      }
  }

  fun retryMonthlyDonationState() {
    if (!monthlyDonationDisposables.isDisposed && store.state.monthlyDonationState.donationStage == DonateToZonaRosaState.DonationStage.FAILURE) {
      store.update { it.copy(monthlyDonationState = it.monthlyDonationState.copy(donationStage = DonateToZonaRosaState.DonationStage.INIT)) }
      initializeMonthlyDonationState(RecurringInAppPaymentRepository)
    }
  }

  fun retryOneTimeDonationState() {
    if (!oneTimeDonationDisposables.isDisposed && store.state.oneTimeDonationState.donationStage == DonateToZonaRosaState.DonationStage.FAILURE) {
      store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(donationStage = DonateToZonaRosaState.DonationStage.INIT)) }
      initializeOneTimeDonationState(OneTimeInAppPaymentRepository)
    }
  }

  fun requestChangeCurrency() {
    val snapshot = store.state
    if (snapshot.canSetCurrency) {
      _actions.onNext(DonateToZonaRosaAction.DisplayCurrencySelectionDialog(snapshot.inAppPaymentType, snapshot.selectableCurrencyCodes))
    }
  }

  fun requestSelectGateway() {
    val snapshot = store.state
    if (snapshot.areFieldsEnabled) {
      actionDisposable += createInAppPayment(snapshot).subscribeBy {
        _actions.onNext(DonateToZonaRosaAction.DisplayGatewaySelectorDialog(it))
      }
    }
  }

  fun updateSubscription() {
    val snapshot = store.state

    check(snapshot.canUpdate)
    if (snapshot.areFieldsEnabled) {
      actionDisposable += createInAppPayment(snapshot).subscribeBy {
        _actions.onNext(DonateToZonaRosaAction.UpdateSubscription(it, snapshot.isUpdateLongRunning))
      }
    }
  }

  fun cancelSubscription() {
    val snapshot = store.state
    if (snapshot.areFieldsEnabled) {
      _actions.onNext(DonateToZonaRosaAction.CancelSubscription)
    }
  }

  fun toggleDonationType() {
    store.update {
      it.copy(
        inAppPaymentType = when (it.inAppPaymentType) {
          InAppPaymentType.ONE_TIME_DONATION -> InAppPaymentType.RECURRING_DONATION
          InAppPaymentType.RECURRING_DONATION -> InAppPaymentType.ONE_TIME_DONATION
          else -> error("Should never get here.")
        }
      )
    }
  }

  fun setSelectedSubscription(subscription: Subscription) {
    store.update { it.copy(monthlyDonationState = it.monthlyDonationState.copy(selectedSubscription = subscription)) }
  }

  fun setSelectedBoost(boost: Boost) {
    store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(selectedBoost = boost, isCustomAmountFocused = false)) }
  }

  fun setCustomAmountFocused() {
    store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(isCustomAmountFocused = true)) }
  }

  fun setCustomAmount(rawAmount: String) {
    val amount = BidiUtil.stripBidiIndicator(rawAmount)
    val bigDecimalAmount: BigDecimal = if (amount.isEmpty() || amount == DecimalFormatSymbols.getInstance().decimalSeparator.toString()) {
      BigDecimal.ZERO
    } else {
      val decimalFormat = DecimalFormat.getInstance() as DecimalFormat
      decimalFormat.isParseBigDecimal = true

      try {
        decimalFormat.parse(amount) as BigDecimal
      } catch (e: NumberFormatException) {
        BigDecimal.ZERO
      }
    }

    store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(customAmount = FiatMoney(bigDecimalAmount, it.oneTimeDonationState.customAmount.currency))) }
  }

  fun getSelectedSubscriptionCost(): FiatMoney {
    return store.state.monthlyDonationState.selectedSubscription!!.prices.first { it.currency == store.state.selectedCurrency }
  }

  fun refreshActiveSubscription() {
    monthlyDonationDisposables += RecurringInAppPaymentRepository
      .getActiveSubscription(InAppPaymentSubscriberRecord.Type.DONATION)
      .subscribeBy(
        onSuccess = {
          _activeSubscription.onNext(it)
        },
        onError = {
          _activeSubscription.onNext(ActiveSubscription.EMPTY)
        }
      )
  }

  private fun createInAppPayment(snapshot: DonateToZonaRosaState): Single<InAppPaymentTable.InAppPayment> {
    val amount = getAmount(snapshot)

    return Single.fromCallable {
      ZonaRosaDatabase.inAppPayments.clearCreated()
      val id = ZonaRosaDatabase.inAppPayments.insert(
        type = snapshot.inAppPaymentType,
        state = InAppPaymentTable.State.CREATED,
        subscriberId = null,
        endOfPeriod = null,
        inAppPaymentData = InAppPaymentData(
          badge = snapshot.badge?.let { Badges.toDatabaseBadge(it) },
          amount = amount.toFiatValue(),
          level = snapshot.level.toLong(),
          recipientId = Recipient.self().id.serialize(),
          paymentMethodType = InAppPaymentData.PaymentMethodType.UNKNOWN,
          redemption = InAppPaymentData.RedemptionState(
            stage = InAppPaymentData.RedemptionState.Stage.INIT
          )
        )
      )

      _inAppPaymentId.onNext(id)
      ZonaRosaDatabase.inAppPayments.getById(id)!!
    }
  }

  private fun getAmount(snapshot: DonateToZonaRosaState): FiatMoney {
    return when (snapshot.inAppPaymentType) {
      InAppPaymentType.ONE_TIME_DONATION -> getOneTimeAmount(snapshot.oneTimeDonationState)
      InAppPaymentType.RECURRING_DONATION -> getSelectedSubscriptionCost()
      else -> error("This ViewModel does not support ${snapshot.inAppPaymentType}.")
    }
  }

  private fun getOneTimeAmount(snapshot: DonateToZonaRosaState.OneTimeDonationState): FiatMoney {
    return if (snapshot.isCustomAmountFocused) {
      snapshot.customAmount
    } else {
      snapshot.selectedBoost!!.price
    }
  }

  private fun initializeOneTimeDonationState(oneTimeInAppPaymentRepository: OneTimeInAppPaymentRepository) {
    val oneTimeDonationFromJob: Observable<Optional<PendingOneTimeDonation>> = InAppPaymentsRepository.observeInAppPaymentRedemption(InAppPaymentType.ONE_TIME_DONATION).map {
      when (it) {
        is DonationRedemptionJobStatus.PendingExternalVerification -> Optional.ofNullable(it.pendingOneTimeDonation)

        DonationRedemptionJobStatus.PendingKeepAlive -> error("Invalid state for one time donation")

        DonationRedemptionJobStatus.PendingReceiptRedemption,
        DonationRedemptionJobStatus.PendingReceiptRequest,
        DonationRedemptionJobStatus.FailedSubscription,
        DonationRedemptionJobStatus.None -> Optional.empty()
      }
    }.distinctUntilChanged()

    val oneTimeDonationFromStore: Observable<Optional<PendingOneTimeDonation>> = ZonaRosaStore.inAppPayments.observablePendingOneTimeDonation
      .map { pending -> pending.filter { !it.isExpired } }
      .distinctUntilChanged()

    oneTimeDonationDisposables += Observable
      .combineLatest(oneTimeDonationFromJob, oneTimeDonationFromStore) { job, store ->
        if (store.isPresent) {
          store
        } else {
          job
        }
      }
      .subscribe { pendingOneTimeDonation: Optional<PendingOneTimeDonation> ->
        store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(pendingOneTimeDonation = pendingOneTimeDonation.orNull())) }
      }

    oneTimeDonationDisposables += oneTimeInAppPaymentRepository.getBoostBadge().subscribeBy(
      onSuccess = { badge ->
        store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(badge = badge)) }
      },
      onError = {
        Log.w(TAG, "Could not load boost badge", it)
      }
    )

    oneTimeDonationDisposables += oneTimeInAppPaymentRepository.getMinimumDonationAmounts().subscribeBy(
      onSuccess = { amountMap ->
        store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(minimumDonationAmounts = amountMap)) }
      },
      onError = {
        Log.w(TAG, "Could not load minimum custom donation amounts.", it)
      }
    )

    val boosts: Observable<Map<Currency, List<Boost>>> = oneTimeInAppPaymentRepository.getBoosts().toObservable()
    val oneTimeCurrency: Observable<Currency> = ZonaRosaStore.inAppPayments.observableOneTimeCurrency

    oneTimeDonationDisposables += Observable.combineLatest(boosts, oneTimeCurrency) { boostMap, currency ->
      val boostList = if (currency in boostMap) {
        boostMap[currency]!!
      } else {
        ZonaRosaStore.inAppPayments.setOneTimeCurrency(PlatformCurrencyUtil.USD)
        listOf()
      }

      Triple(boostList, currency, boostMap.keys)
    }.subscribeBy(
      onNext = { (boostList, currency, availableCurrencies) ->
        store.update { state ->
          state.copy(
            oneTimeDonationState = state.oneTimeDonationState.copy(
              boosts = boostList,
              selectedBoost = null,
              selectedCurrency = currency,
              donationStage = DonateToZonaRosaState.DonationStage.READY,
              selectableCurrencyCodes = availableCurrencies.map(Currency::getCurrencyCode),
              isCustomAmountFocused = false,
              customAmount = FiatMoney(
                BigDecimal.ZERO,
                currency
              )
            )
          )
        }
      },
      onError = {
        Log.w(TAG, "Could not load boost information", it)
        store.update { it.copy(oneTimeDonationState = it.oneTimeDonationState.copy(donationStage = DonateToZonaRosaState.DonationStage.FAILURE)) }
      }
    )
  }

  private fun initializeMonthlyDonationState(subscriptionsRepository: RecurringInAppPaymentRepository) {
    monitorLevelUpdateProcessing()

    val allSubscriptions = subscriptionsRepository.getSubscriptions()
    ensureValidSubscriptionCurrency(allSubscriptions)
    monitorSubscriptionCurrency()
    monitorSubscriptionState(allSubscriptions)
    refreshActiveSubscription()
  }

  private fun monitorLevelUpdateProcessing() {
    val redemptionJobStatus: Observable<DonationRedemptionJobStatus> = InAppPaymentsRepository.observeInAppPaymentRedemption(InAppPaymentType.RECURRING_DONATION)

    monthlyDonationDisposables += Observable
      .combineLatest(redemptionJobStatus, LevelUpdate.isProcessing, ::Pair)
      .subscribeBy { (jobStatus, levelUpdateProcessing) ->
        store.update { state ->
          state.copy(
            monthlyDonationState = state.monthlyDonationState.copy(
              nonVerifiedMonthlyDonation = if (jobStatus is DonationRedemptionJobStatus.PendingExternalVerification) jobStatus.nonVerifiedMonthlyDonation else null,
              transactionState = DonateToZonaRosaState.TransactionState(jobStatus.isInProgress(), levelUpdateProcessing, jobStatus is DonationRedemptionJobStatus.PendingKeepAlive)
            )
          )
        }
      }
  }

  private fun monitorSubscriptionState(allSubscriptions: Single<List<Subscription>>) {
    monthlyDonationDisposables += Observable.combineLatest(allSubscriptions.toObservable(), _activeSubscription, ::Pair).subscribeBy(
      onNext = { (subs, active) ->
        store.update { state ->
          state.copy(
            monthlyDonationState = state.monthlyDonationState.copy(
              subscriptions = subs,
              selectedSubscription = state.monthlyDonationState.selectedSubscription ?: resolveSelectedSubscription(active, subs),
              _activeSubscription = active,
              donationStage = DonateToZonaRosaState.DonationStage.READY,
              selectableCurrencyCodes = subs.firstOrNull()?.prices?.map { it.currency.currencyCode } ?: emptyList()
            )
          )
        }
      },
      onError = {
        store.update { state ->
          state.copy(
            monthlyDonationState = state.monthlyDonationState.copy(
              donationStage = DonateToZonaRosaState.DonationStage.FAILURE
            )
          )
        }
      }
    )
  }

  private fun resolveSelectedSubscription(activeSubscription: ActiveSubscription, subscriptions: List<Subscription>): Subscription? {
    return if (activeSubscription.isActive) {
      subscriptions.firstOrNull { it.level == activeSubscription.activeSubscription.level }
    } else {
      subscriptions.firstOrNull()
    }
  }

  private fun ensureValidSubscriptionCurrency(allSubscriptions: Single<List<Subscription>>) {
    monthlyDonationDisposables += allSubscriptions.subscribeBy(
      onSuccess = { subscriptions ->
        if (subscriptions.isNotEmpty()) {
          val priceCurrencies = subscriptions[0].prices.map { it.currency }
          val selectedCurrency = ZonaRosaStore.inAppPayments.getRecurringDonationCurrency()

          if (selectedCurrency !in priceCurrencies) {
            Log.w(TAG, "Unsupported currency selection. Defaulting to USD. $selectedCurrency isn't supported.")
            val usd = PlatformCurrencyUtil.USD
            val newSubscriber = InAppPaymentsRepository.getRecurringDonationSubscriber(usd) ?: InAppPaymentSubscriberRecord(
              subscriberId = SubscriberId.generate(),
              currency = usd,
              type = InAppPaymentSubscriberRecord.Type.DONATION,
              requiresCancel = false,
              paymentMethodType = InAppPaymentData.PaymentMethodType.UNKNOWN,
              iapSubscriptionId = null
            )
            InAppPaymentsRepository.setSubscriber(newSubscriber)
            RecurringInAppPaymentRepository.syncAccountRecord().subscribe()
          }
        }
      },
      onError = {}
    )
  }

  private fun monitorSubscriptionCurrency() {
    monthlyDonationDisposables += ZonaRosaStore.inAppPayments.observableRecurringDonationCurrency.subscribe {
      store.update { state ->
        state.copy(monthlyDonationState = state.monthlyDonationState.copy(selectedCurrency = it))
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    oneTimeDonationDisposables.clear()
    monthlyDonationDisposables.clear()
    networkDisposable.clear()
    actionDisposable.clear()
    store.dispose()
  }

  class Factory(
    private val startType: InAppPaymentType
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(DonateToZonaRosaViewModel(startType)) as T
    }
  }
}
