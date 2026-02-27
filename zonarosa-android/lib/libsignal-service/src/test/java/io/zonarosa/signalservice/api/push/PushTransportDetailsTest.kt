/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.push

import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.Test
import io.zonarosa.service.internal.push.PushTransportDetails

class PushTransportDetailsTest {
  private val transportV3 = PushTransportDetails()

  @Test
  fun testV3Padding() {
    (0 until 79).forEach { i ->
      val message = ByteArray(i)
      assertThat(transportV3.getPaddedMessageBody(message)).hasSize(79)
    }

    (79 until 159).forEach { i ->
      val message = ByteArray(i)
      assertThat(transportV3.getPaddedMessageBody(message)).hasSize(159)
    }

    (159 until 239).forEach { i ->
      val message = ByteArray(i)
      assertThat(transportV3.getPaddedMessageBody(message)).hasSize(239)
    }
  }
}
