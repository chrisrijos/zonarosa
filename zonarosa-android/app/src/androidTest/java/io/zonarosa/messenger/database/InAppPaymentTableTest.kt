/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.util.deleteAll
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.testing.ZonaRosaActivityRule

@RunWith(AndroidJUnit4::class)
class InAppPaymentTableTest {
  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Before
  fun setUp() {
    ZonaRosaDatabase.inAppPayments.writableDatabase.deleteAll(InAppPaymentTable.TABLE_NAME)
  }

  @Test
  fun givenACreatedInAppPayment_whenIUpdateToPending_thenIExpectPendingPayment() {
    val inAppPaymentId = ZonaRosaDatabase.inAppPayments.insert(
      type = InAppPaymentType.ONE_TIME_DONATION,
      state = InAppPaymentTable.State.CREATED,
      subscriberId = null,
      endOfPeriod = null,
      inAppPaymentData = InAppPaymentData()
    )

    val paymentBeforeUpdate = ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)
    assertThat(paymentBeforeUpdate?.state).isEqualTo(InAppPaymentTable.State.CREATED)

    ZonaRosaDatabase.inAppPayments.update(
      inAppPayment = paymentBeforeUpdate!!.copy(state = InAppPaymentTable.State.PENDING)
    )

    val paymentAfterUpdate = ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)
    assertThat(paymentAfterUpdate?.state).isEqualTo(InAppPaymentTable.State.PENDING)
  }
}
