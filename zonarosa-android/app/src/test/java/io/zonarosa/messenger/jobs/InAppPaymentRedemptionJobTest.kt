/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import android.app.Application
import assertk.assertThat
import assertk.assertions.isTrue
import io.mockk.every
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.messenger.testutil.MockZonaRosaStoreRule

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class InAppPaymentRedemptionJobTest {

  @get:Rule
  val mockZonaRosaStore = MockZonaRosaStoreRule()

  @Test
  fun `Given an unregistered local user, when I run, then I expect failure`() {
    every { mockZonaRosaStore.account.isRegistered } returns false

    val job = InAppPaymentRedemptionJob.create()

    val result = job.run()

    assertThat(result.isFailure).isTrue()
  }
}
