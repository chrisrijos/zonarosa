/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.errors

import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.donations.StripeDeclineCode
import io.zonarosa.donations.StripeFailureCode
import io.zonarosa.service.api.subscriptions.ActiveSubscription
import io.zonarosa.service.internal.push.exceptions.InAppPaymentProcessorError

fun InAppPaymentProcessorError.toDonationError(
  source: DonationErrorSource,
  method: PaymentSourceType
): DonationError {
  return when (processor) {
    ActiveSubscription.Processor.STRIPE -> {
      check(method is PaymentSourceType.Stripe)
      val declineCode = StripeDeclineCode.getFromCode(chargeFailure.code)
      val failureCode = StripeFailureCode.getFromCode(chargeFailure.code)
      if (declineCode.isKnown()) {
        DonationError.PaymentSetupError.StripeDeclinedError(source, this, declineCode, method)
      } else if (failureCode.isKnown) {
        DonationError.PaymentSetupError.StripeFailureCodeError(source, this, failureCode, method)
      } else if (chargeFailure.code != null) {
        DonationError.PaymentSetupError.StripeCodedError(source, this, chargeFailure.code)
      } else {
        DonationError.PaymentSetupError.GenericError(source, this)
      }
    }
    ActiveSubscription.Processor.BRAINTREE -> {
      check(method is PaymentSourceType.PayPal)
      val code = chargeFailure.code
      if (code == null) {
        DonationError.PaymentSetupError.GenericError(source, this)
      } else {
        val declineCode = PayPalDeclineCode.KnownCode.fromCode(code.toInt())
        if (declineCode != null) {
          DonationError.PaymentSetupError.PayPalDeclinedError(source, this, declineCode)
        } else {
          DonationError.PaymentSetupError.PayPalCodedError(source, this, code.toInt())
        }
      }
    }
    ActiveSubscription.Processor.GOOGLE_PLAY_BILLING -> {
      check(method is PaymentSourceType.GooglePlayBilling)
      DonationError.PaymentSetupError.GenericError(source, this)
    }
  }
}
