/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import assertk.assertThat
import assertk.assertions.isTrue
import io.mockk.every
import org.junit.Rule
import org.junit.Test
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsTestRule
import io.zonarosa.messenger.testutil.MockZonaRosaStoreRule

class InAppPaymentOneTimeContextJobTest {

  @get:Rule
  val mockZonaRosaStore = MockZonaRosaStoreRule()

  @get:Rule
  val iapRule = InAppPaymentsTestRule()

  @Test
  fun `Given an unregistered local user, when I run, then I expect failure`() {
    every { mockZonaRosaStore.account.isRegistered } returns false

    val job = InAppPaymentOneTimeContextJob.create(iapRule.createInAppPayment(InAppPaymentType.ONE_TIME_DONATION, PaymentSourceType.PayPal))

    val result = job.run()

    assertThat(result.isFailure).isTrue()
  }
}
