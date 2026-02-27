/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push.exceptions

import com.fasterxml.jackson.annotation.JsonCreator
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException
import io.zonarosa.service.api.subscriptions.ActiveSubscription.ChargeFailure

/**
 * HTTP 402 Exception when trying to submit credentials for a donation with
 * a failed payment.
 */
class InAppPaymentReceiptCredentialError @JsonCreator constructor(
  val chargeFailure: ChargeFailure
) : NonSuccessfulResponseCodeException(402) {
  override fun toString(): String {
    return """
      DonationReceiptCredentialError (402)
      Charge Failure: $chargeFailure
    """.trimIndent()
  }
}
