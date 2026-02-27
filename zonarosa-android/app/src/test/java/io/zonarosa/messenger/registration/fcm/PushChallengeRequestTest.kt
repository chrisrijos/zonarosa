/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.registration.fcm

import android.app.Application
import android.os.AsyncTask
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isPresent
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.messenger.isAbsent
import io.zonarosa.service.api.ZonaRosaServiceAccountManager
import java.io.IOException
import java.util.Optional

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class PushChallengeRequestTest {
  @Test
  fun pushChallengeBlocking_returns_absent_if_times_out() {
    val zonarosa = mockk<ZonaRosaServiceAccountManager>(relaxUnitFun = true)

    val challenge = PushChallengeRequest.getPushChallengeBlocking(zonarosa, "session ID", Optional.of("token"), 50L)

    assertThat(challenge).isAbsent()
  }

  @Test
  fun pushChallengeBlocking_waits_for_specified_period() {
    val zonarosa = mockk<ZonaRosaServiceAccountManager>(relaxUnitFun = true)

    val startTime = System.currentTimeMillis()
    PushChallengeRequest.getPushChallengeBlocking(zonarosa, "session ID", Optional.of("token"), 250L)
    val duration = System.currentTimeMillis() - startTime

    assertThat(duration).isGreaterThanOrEqualTo(250L)
  }

  @Test
  fun pushChallengeBlocking_completes_fast_if_posted_to_event_bus() {
    val zonarosa = mockk<ZonaRosaServiceAccountManager> {
      every {
        requestRegistrationPushChallenge("session ID", "token")
      } answers {
        AsyncTask.execute { PushChallengeRequest.postChallengeResponse("CHALLENGE") }
      }
    }

    val startTime = System.currentTimeMillis()
    val challenge = PushChallengeRequest.getPushChallengeBlocking(zonarosa, "session ID", Optional.of("token"), 500L)
    val duration = System.currentTimeMillis() - startTime

    assertThat(duration).isLessThan(500L)
    verify { zonarosa.requestRegistrationPushChallenge("session ID", "token") }
    confirmVerified(zonarosa)

    assertThat(challenge).isPresent().isEqualTo("CHALLENGE")
  }

  @Test
  fun pushChallengeBlocking_returns_fast_if_no_fcm_token_supplied() {
    val zonarosa = mockk<ZonaRosaServiceAccountManager>()

    val startTime = System.currentTimeMillis()
    PushChallengeRequest.getPushChallengeBlocking(zonarosa, "session ID", Optional.empty(), 500L)
    val duration = System.currentTimeMillis() - startTime

    assertThat(duration).isLessThan(500L)
  }

  @Test
  fun pushChallengeBlocking_returns_absent_if_no_fcm_token_supplied() {
    val zonarosa = mockk<ZonaRosaServiceAccountManager>()

    val challenge = PushChallengeRequest.getPushChallengeBlocking(zonarosa, "session ID", Optional.empty(), 500L)

    verify { zonarosa wasNot called }
    assertThat(challenge).isAbsent()
  }

  @Test
  fun pushChallengeBlocking_returns_absent_if_any_IOException_is_thrown() {
    val zonarosa = mockk<ZonaRosaServiceAccountManager> {
      every { requestRegistrationPushChallenge(any(), any()) } throws IOException()
    }

    val challenge = PushChallengeRequest.getPushChallengeBlocking(zonarosa, "session ID", Optional.of("token"), 500L)

    assertThat(challenge).isAbsent()
  }
}
