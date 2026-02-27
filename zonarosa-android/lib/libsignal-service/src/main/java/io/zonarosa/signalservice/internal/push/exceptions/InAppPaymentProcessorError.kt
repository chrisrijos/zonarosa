/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push.exceptions

import com.fasterxml.jackson.annotation.JsonCreator
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException
import io.zonarosa.service.api.subscriptions.ActiveSubscription.ChargeFailure
import io.zonarosa.service.api.subscriptions.ActiveSubscription.Processor

/**
 * HTTP 440 Exception when something bad happens while updating a user's subscription level or
 * confirming a PayPal intent.
 */
class InAppPaymentProcessorError @JsonCreator constructor(
  val processor: Processor,
  val chargeFailure: ChargeFailure
) : NonSuccessfulResponseCodeException(440) {
  override fun toString(): String {
    return """
      DonationProcessorError (440)
      Processor: $processor
      Charge Failure: $chargeFailure
    """.trimIndent()
  }
}
