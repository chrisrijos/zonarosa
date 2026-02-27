package io.zonarosa.messenger.jobs

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.util.deleteAll
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.components.settings.app.subscription.DonationSerializationHelper.toFiatValue
import io.zonarosa.messenger.database.DonationReceiptTable
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentReceiptRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import java.math.BigDecimal
import java.util.Currency

@RunWith(AndroidJUnit4::class)
class InAppPaymentAuthCheckJobTest {

  companion object {
    private const val TEST_INTENT_ID = "test-intent-id"
    private const val TEST_CLIENT_SECRET = "test-client-secret"
  }

  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Before
  fun setUp() {
    ZonaRosaDatabase.inAppPayments.writableDatabase.deleteAll(InAppPaymentTable.TABLE_NAME)
    ZonaRosaDatabase.donationReceipts.writableDatabase.deleteAll(DonationReceiptTable.TABLE_NAME)
  }

  @Test
  fun givenCanceledOneTimeAuthRequiredPayment_whenICheck_thenIDoNotExpectAReceipt() {
    ZonaRosaDatabase.inAppPayments.insert(
      type = InAppPaymentType.ONE_TIME_DONATION,
      state = InAppPaymentTable.State.WAITING_FOR_AUTHORIZATION,
      subscriberId = null,
      endOfPeriod = null,
      inAppPaymentData = InAppPaymentData(
        amount = FiatMoney(BigDecimal.ONE, Currency.getInstance("USD")).toFiatValue(),
        waitForAuth = InAppPaymentData.WaitingForAuthorizationState(
          stripeIntentId = TEST_INTENT_ID,
          stripeClientSecret = TEST_CLIENT_SECRET
        )
      )
    )

    InAppPaymentAuthCheckJob().run()

    val receipts = ZonaRosaDatabase.donationReceipts.getReceipts(InAppPaymentReceiptRecord.Type.ONE_TIME_DONATION)
    assertThat(receipts).isEmpty()
  }
}
