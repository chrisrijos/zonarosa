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
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.testutil.MockZonaRosaStoreRule

class InAppPaymentKeepAliveJobTest {

  @get:Rule
  val mockZonaRosaStore = MockZonaRosaStoreRule()

  @Test
  fun `Given an unregistered local user, when I run, then I expect skip`() {
    every { mockZonaRosaStore.account.isRegistered } returns false

    val job = InAppPaymentKeepAliveJob.create(InAppPaymentSubscriberRecord.Type.DONATION)

    val result = job.run()

    assertThat(result.isSuccess).isTrue()
  }
}
