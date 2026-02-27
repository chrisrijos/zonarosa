/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.donate.transfer.mandate

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.toPaymentSourceType
import io.zonarosa.messenger.components.settings.app.subscription.donate.transfer.details.BankTransferDetailsViewModel
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData

class BankTransferMandateViewModel(
  private val inAppPaymentId: InAppPaymentTable.InAppPaymentId
) : ViewModel() {

  companion object {
    private val TAG = Log.tag(BankTransferDetailsViewModel::class)
  }

  private val disposables = CompositeDisposable()
  private val internalMandate = mutableStateOf("")
  private val internalFailedToLoadMandate = mutableStateOf(false)

  val mandate: State<String> = internalMandate
  val failedToLoadMandate: State<Boolean> = internalFailedToLoadMandate

  init {
    val inAppPayment = InAppPaymentsRepository.requireInAppPayment(inAppPaymentId)

    disposables += inAppPayment
      .flatMap {
        BankTransferMandateRepository.getMandate(it.data.paymentMethodType.toPaymentSourceType() as PaymentSourceType.Stripe)
      }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeBy(
        onSuccess = { internalMandate.value = it },
        onError = {
          Log.w(TAG, "Failed to load mandate.", it)
          internalFailedToLoadMandate.value = true
        }
      )
  }

  suspend fun getPaymentMethodType(): InAppPaymentData.PaymentMethodType {
    return withContext(ZonaRosaDispatchers.IO) {
      ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)!!.data.paymentMethodType
    }
  }

  override fun onCleared() {
    disposables.clear()
  }
}
