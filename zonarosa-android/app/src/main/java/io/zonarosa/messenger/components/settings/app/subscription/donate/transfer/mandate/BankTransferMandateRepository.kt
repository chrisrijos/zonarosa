/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.donate.transfer.mandate

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.messenger.dependencies.AppDependencies
import java.util.Locale

object BankTransferMandateRepository {

  fun getMandate(paymentSourceType: PaymentSourceType.Stripe): Single<String> {
    val sourceString = if (paymentSourceType == PaymentSourceType.Stripe.IDEAL) {
      PaymentSourceType.Stripe.SEPADebit.paymentMethod
    } else {
      paymentSourceType.paymentMethod
    }

    return Single
      .fromCallable { AppDependencies.donationsService.getBankMandate(Locale.getDefault(), sourceString) }
      .flatMap { it.flattenResult() }
      .map { it.mandate }
      .subscribeOn(Schedulers.io())
  }
}
