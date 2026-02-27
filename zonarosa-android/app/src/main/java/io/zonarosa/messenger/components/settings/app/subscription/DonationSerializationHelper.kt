/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription

import okio.ByteString
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.database.model.databaseprotos.DecimalValue
import io.zonarosa.messenger.database.model.databaseprotos.FiatValue
import io.zonarosa.messenger.database.model.databaseprotos.PendingOneTimeDonation
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.Currency

object DonationSerializationHelper {
  fun createPendingOneTimeDonationProto(
    badge: Badge,
    paymentSourceType: PaymentSourceType,
    amount: FiatMoney
  ): PendingOneTimeDonation {
    return PendingOneTimeDonation(
      badge = Badges.toDatabaseBadge(badge),
      paymentMethodType = when (paymentSourceType) {
        PaymentSourceType.GooglePlayBilling -> error("Unsupported payment source.")
        PaymentSourceType.PayPal -> PendingOneTimeDonation.PaymentMethodType.PAYPAL
        PaymentSourceType.Stripe.CreditCard, PaymentSourceType.Stripe.GooglePay, PaymentSourceType.Unknown -> PendingOneTimeDonation.PaymentMethodType.CARD
        PaymentSourceType.Stripe.SEPADebit -> PendingOneTimeDonation.PaymentMethodType.SEPA_DEBIT
        PaymentSourceType.Stripe.IDEAL -> PendingOneTimeDonation.PaymentMethodType.IDEAL
      },
      amount = amount.toFiatValue(),
      timestamp = System.currentTimeMillis()
    )
  }

  fun FiatValue.toFiatMoney(): FiatMoney {
    return FiatMoney(
      amount!!.toBigDecimal(),
      Currency.getInstance(currencyCode)
    )
  }

  fun DecimalValue.toBigDecimal(): BigDecimal {
    return BigDecimal(
      BigInteger(value_.toByteArray()),
      scale,
      MathContext(precision)
    )
  }

  fun FiatMoney.toFiatValue(): FiatValue {
    return FiatValue(
      currencyCode = currency.currencyCode,
      amount = amount.toDecimalValue()
    )
  }

  fun BigDecimal.toDecimalValue(): DecimalValue {
    return DecimalValue(
      scale = scale(),
      precision = precision(),
      value_ = ByteString.of(*this.unscaledValue().toByteArray())
    )
  }
}
