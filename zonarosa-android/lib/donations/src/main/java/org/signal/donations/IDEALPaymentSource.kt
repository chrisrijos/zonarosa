/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.donations

class IDEALPaymentSource(
  val idealData: StripeApi.IDEALData
) : PaymentSource {
  override val type: PaymentSourceType = PaymentSourceType.Stripe.IDEAL

  override fun email(): String? = null
}
