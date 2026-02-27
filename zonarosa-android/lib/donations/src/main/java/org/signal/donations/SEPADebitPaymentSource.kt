/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.donations

class SEPADebitPaymentSource(
  val sepaDebitData: StripeApi.SEPADebitData
) : PaymentSource {
  override val type: PaymentSourceType = PaymentSourceType.Stripe.SEPADebit

  override fun email(): String? = null
}
