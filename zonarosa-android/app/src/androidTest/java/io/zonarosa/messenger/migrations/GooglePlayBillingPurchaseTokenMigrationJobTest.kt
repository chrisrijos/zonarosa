package io.zonarosa.messenger.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.util.billing.BillingPurchaseResult
import io.zonarosa.core.util.billing.BillingPurchaseState
import io.zonarosa.core.util.billing.BillingResponseCode
import io.zonarosa.core.util.deleteAll
import io.zonarosa.messenger.database.InAppPaymentSubscriberTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.service.api.storage.IAPSubscriptionId
import io.zonarosa.service.api.subscriptions.SubscriberId

@RunWith(AndroidJUnit4::class)
class GooglePlayBillingPurchaseTokenMigrationJobTest {
  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Before
  fun setUp() {
    ZonaRosaDatabase.inAppPaymentSubscribers.writableDatabase.deleteAll(InAppPaymentSubscriberTable.TABLE_NAME)
  }

  @Test
  fun givenNoSubscribers_whenIRunJob_thenIExpectNoBillingAccess() {
    val job = GooglePlayBillingPurchaseTokenMigrationJob()

    job.run()

    verify { AppDependencies.billingApi wasNot Called }
  }

  @Test
  fun givenSubscriberWithAppleData_whenIRunJob_thenIExpectNoBillingAccess() {
    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(
      InAppPaymentSubscriberRecord(
        subscriberId = SubscriberId.generate(),
        currency = null,
        type = InAppPaymentSubscriberRecord.Type.BACKUP,
        requiresCancel = false,
        paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
        iapSubscriptionId = IAPSubscriptionId.AppleIAPOriginalTransactionId(1000L)
      )
    )

    val job = GooglePlayBillingPurchaseTokenMigrationJob()

    job.run()

    verify { AppDependencies.billingApi wasNot Called }
  }

  @Test
  fun givenSubscriberWithGoogleToken_whenIRunJob_thenIExpectNoBillingAccess() {
    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(
      InAppPaymentSubscriberRecord(
        subscriberId = SubscriberId.generate(),
        currency = null,
        type = InAppPaymentSubscriberRecord.Type.BACKUP,
        requiresCancel = false,
        paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
        iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("testToken")
      )
    )

    val job = GooglePlayBillingPurchaseTokenMigrationJob()

    job.run()

    verify { AppDependencies.billingApi wasNot Called }
  }

  @Test
  fun givenSubscriberWithPlaceholderAndNoBillingAccess_whenIRunJob_thenIExpectNoUpdate() {
    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(
      InAppPaymentSubscriberRecord(
        subscriberId = SubscriberId.generate(),
        currency = null,
        type = InAppPaymentSubscriberRecord.Type.BACKUP,
        requiresCancel = false,
        paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
        iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("-")
      )
    )

    coEvery { AppDependencies.billingApi.getApiAvailability() } returns BillingResponseCode.BILLING_UNAVAILABLE

    val job = GooglePlayBillingPurchaseTokenMigrationJob()

    job.run()

    val sub = ZonaRosaDatabase.inAppPaymentSubscribers.getBackupsSubscriber()

    assertThat(sub?.iapSubscriptionId?.purchaseToken).isEqualTo("-")
  }

  @Test
  fun givenSubscriberWithPlaceholderAndNoPurchase_whenIRunJob_thenIExpectNoUpdate() {
    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(
      InAppPaymentSubscriberRecord(
        subscriberId = SubscriberId.generate(),
        currency = null,
        type = InAppPaymentSubscriberRecord.Type.BACKUP,
        requiresCancel = false,
        paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
        iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("-")
      )
    )

    coEvery { AppDependencies.billingApi.getApiAvailability() } returns BillingResponseCode.OK
    coEvery { AppDependencies.billingApi.queryPurchases() } returns BillingPurchaseResult.None

    val job = GooglePlayBillingPurchaseTokenMigrationJob()

    job.run()

    val sub = ZonaRosaDatabase.inAppPaymentSubscribers.getBackupsSubscriber()

    assertThat(sub?.iapSubscriptionId?.purchaseToken).isEqualTo("-")
  }

  @Test
  fun givenSubscriberWithPurchase_whenIRunJob_thenIExpectUpdate() {
    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(
      InAppPaymentSubscriberRecord(
        subscriberId = SubscriberId.generate(),
        currency = null,
        type = InAppPaymentSubscriberRecord.Type.BACKUP,
        requiresCancel = false,
        paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
        iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("-")
      )
    )

    coEvery { AppDependencies.billingApi.getApiAvailability() } returns BillingResponseCode.OK
    coEvery { AppDependencies.billingApi.queryPurchases() } returns BillingPurchaseResult.Success(
      purchaseState = BillingPurchaseState.PURCHASED,
      purchaseToken = "purchaseToken",
      isAcknowledged = true,
      purchaseTime = System.currentTimeMillis(),
      isAutoRenewing = true
    )

    val job = GooglePlayBillingPurchaseTokenMigrationJob()

    job.run()

    val sub = ZonaRosaDatabase.inAppPaymentSubscribers.getBackupsSubscriber()

    assertThat(sub?.iapSubscriptionId?.purchaseToken).isEqualTo("purchaseToken")
  }
}
