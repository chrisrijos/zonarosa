/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.badges.self.expired

import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.donations.StripeDeclineCode
import io.zonarosa.donations.StripeFailureCode
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.toActiveSubscriptionChargeFailure
import io.zonarosa.messenger.components.settings.app.subscription.errors.mapToErrorStringResource
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.service.api.subscriptions.ActiveSubscription.ChargeFailure

class MonthlyDonationCanceledViewModel(
  inAppPaymentId: InAppPaymentTable.InAppPaymentId?
) : ViewModel() {
  private val internalState = mutableStateOf(MonthlyDonationCanceledState())
  val state: State<MonthlyDonationCanceledState> = internalState

  init {
    if (inAppPaymentId != null) {
      initializeFromInAppPaymentId(inAppPaymentId)
    } else {
      initializeFromZonaRosaStore()
    }
  }

  private fun initializeFromInAppPaymentId(inAppPaymentId: InAppPaymentTable.InAppPaymentId) {
    viewModelScope.launch {
      val inAppPayment = withContext(Dispatchers.IO) {
        ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)
      }

      if (inAppPayment != null) {
        internalState.value = MonthlyDonationCanceledState(
          loadState = MonthlyDonationCanceledState.LoadState.READY,
          badge = Badges.fromDatabaseBadge(inAppPayment.data.badge!!),
          errorMessage = getErrorMessage(inAppPayment.data.cancellation?.chargeFailure?.toActiveSubscriptionChargeFailure())
        )
      } else {
        internalState.value = internalState.value.copy(loadState = MonthlyDonationCanceledState.LoadState.FAILED)
      }
    }
  }

  private fun initializeFromZonaRosaStore() {
    internalState.value = MonthlyDonationCanceledState(
      loadState = MonthlyDonationCanceledState.LoadState.READY,
      badge = ZonaRosaStore.inAppPayments.getExpiredBadge(),
      errorMessage = getErrorMessage(ZonaRosaStore.inAppPayments.getUnexpectedSubscriptionCancelationChargeFailure())
    )
  }

  @StringRes
  private fun getErrorMessage(chargeFailure: ChargeFailure?): Int {
    val declineCode: StripeDeclineCode = StripeDeclineCode.getFromCode(chargeFailure?.outcomeNetworkReason)
    val failureCode: StripeFailureCode = StripeFailureCode.getFromCode(chargeFailure?.code)

    return if (declineCode.isKnown()) {
      declineCode.mapToErrorStringResource()
    } else if (failureCode.isKnown) {
      failureCode.mapToErrorStringResource(InAppPaymentType.RECURRING_DONATION)
    } else {
      declineCode.mapToErrorStringResource()
    }
  }
}
