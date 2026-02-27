/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.donate.gateway

import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.zonarosa.core.util.orNull
import io.zonarosa.messenger.components.settings.app.subscription.InAppDonations
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.recipients.Recipient

sealed interface GatewayOrderStrategy {

  val orderedGateways: Set<InAppPaymentData.PaymentMethodType>

  private object Default : GatewayOrderStrategy {
    override val orderedGateways: Set<InAppPaymentData.PaymentMethodType> = setOf(
      InAppPaymentData.PaymentMethodType.CARD,
      InAppPaymentData.PaymentMethodType.PAYPAL,
      InAppPaymentData.PaymentMethodType.GOOGLE_PAY,
      InAppPaymentData.PaymentMethodType.SEPA_DEBIT,
      InAppPaymentData.PaymentMethodType.IDEAL
    )
  }

  private object NorthAmerica : GatewayOrderStrategy {
    override val orderedGateways: Set<InAppPaymentData.PaymentMethodType> = setOf(
      InAppPaymentData.PaymentMethodType.GOOGLE_PAY,
      InAppPaymentData.PaymentMethodType.PAYPAL,
      InAppPaymentData.PaymentMethodType.CARD,
      InAppPaymentData.PaymentMethodType.SEPA_DEBIT,
      InAppPaymentData.PaymentMethodType.IDEAL
    )
  }

  private object Netherlands : GatewayOrderStrategy {
    override val orderedGateways: Set<InAppPaymentData.PaymentMethodType> = setOf(
      InAppPaymentData.PaymentMethodType.IDEAL,
      InAppPaymentData.PaymentMethodType.PAYPAL,
      InAppPaymentData.PaymentMethodType.GOOGLE_PAY,
      InAppPaymentData.PaymentMethodType.CARD,
      InAppPaymentData.PaymentMethodType.SEPA_DEBIT
    )
  }

  companion object {
    fun getStrategy(): GatewayOrderStrategy {
      val self = Recipient.self()
      val e164 = self.e164.orNull() ?: return Default

      return if (PhoneNumberUtil.getInstance().parse(e164, "").countryCode == 1) {
        NorthAmerica
      } else if (InAppDonations.isIDEALAvailable()) {
        Netherlands
      } else {
        Default
      }
    }
  }
}
