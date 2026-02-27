/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription

import android.app.Activity
import android.content.Intent
import io.reactivex.rxjava3.core.Completable
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.GooglePayApi
import io.zonarosa.donations.StripeApi
import io.zonarosa.messenger.util.Environment

/**
 * Extraction of components that only deal with GooglePay from StripeRepository
 */
class GooglePayRepository(activity: Activity) {

  companion object {
    private val TAG = Log.tag(GooglePayRepository::class)
  }

  private val googlePayApi = GooglePayApi(activity, StripeApi.Gateway(Environment.Donations.STRIPE_CONFIGURATION), Environment.Donations.GOOGLE_PAY_CONFIGURATION)

  fun isGooglePayAvailable(): Completable {
    return googlePayApi.queryIsReadyToPay()
  }

  fun requestTokenFromGooglePay(price: FiatMoney, label: String, requestCode: Int) {
    Log.d(TAG, "Requesting a token from google pay...")
    googlePayApi.requestPayment(price, label, requestCode)
  }

  fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    expectedRequestCode: Int,
    paymentsRequestCallback: GooglePayApi.PaymentRequestCallback
  ) {
    Log.d(TAG, "Processing possible google pay result...")
    googlePayApi.onActivityResult(requestCode, resultCode, data, expectedRequestCode, paymentsRequestCallback)
  }
}
