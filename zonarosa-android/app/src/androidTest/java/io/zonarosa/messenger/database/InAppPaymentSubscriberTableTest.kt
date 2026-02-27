package io.zonarosa.messenger.database

import android.database.sqlite.SQLiteConstraintException
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.zonarosa.core.util.count
import io.zonarosa.core.util.deleteAll
import io.zonarosa.core.util.readToSingleInt
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.service.api.storage.IAPSubscriptionId
import io.zonarosa.service.api.subscriptions.SubscriberId
import java.util.Currency

class InAppPaymentSubscriberTableTest {
  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Before
  fun setUp() {
    ZonaRosaDatabase.inAppPaymentSubscribers.writableDatabase.deleteAll(InAppPaymentTable.TABLE_NAME)
  }

  @Test(expected = SQLiteConstraintException::class)
  fun givenASubscriberWithCurrencyAndIAPData_whenITryToInsert_thenIExpectException() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = Currency.getInstance("USD"),
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("testToken")
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)

    fail("Expected a thrown exception.")
  }

  @Test(expected = SQLiteConstraintException::class)
  fun givenADonorSubscriberWithGoogleIAPData_whenITryToInsert_thenIExpectException() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("testToken")
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)

    fail("Expected a thrown exception.")
  }

  @Test(expected = SQLiteConstraintException::class)
  fun givenADonorSubscriberWithAppleIAPData_whenITryToInsert_thenIExpectException() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = IAPSubscriptionId.AppleIAPOriginalTransactionId(1000L)
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)

    fail("Expected a thrown exception.")
  }

  @Test(expected = SQLiteConstraintException::class)
  fun givenADonorSubscriberWithoutCurrency_whenITryToInsert_thenIExpectException() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = null
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)

    fail("Expected a thrown exception.")
  }

  @Test
  fun givenADonorSubscriberWithCurrency_whenITryToInsert_thenIExpectSuccess() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = Currency.getInstance("USD"),
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = null
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)
  }

  @Test(expected = SQLiteConstraintException::class)
  fun givenABackupSubscriberWithCurrency_whenITryToInsert_thenIExpectException() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = Currency.getInstance("USD"),
      type = InAppPaymentSubscriberRecord.Type.BACKUP,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
      iapSubscriptionId = null
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)

    fail("Expected a thrown exception.")
  }

  @Test(expected = SQLiteConstraintException::class)
  fun givenABackupSubscriberWithoutIAPData_whenITryToInsert_thenIExpectException() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.BACKUP,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
      iapSubscriptionId = null
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)
  }

  @Test
  fun givenABackupSubscriberWithGoogleIAPData_whenITryToInsert_thenIExpectSuccess() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.BACKUP,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
      iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("testToken")
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)
  }

  @Test
  fun givenABackupSubscriberWithAppleIAPData_whenITryToInsert_thenIExpectSuccess() {
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.BACKUP,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
      iapSubscriptionId = IAPSubscriptionId.AppleIAPOriginalTransactionId(1000L)
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(subscriber)
  }

  @Test
  fun givenABackupSubscriberWithAppleIAPData_whenITryToInsertAGoogleSubscriber_thenIExpectSuccess() {
    val appleSubscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.BACKUP,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
      iapSubscriptionId = IAPSubscriptionId.AppleIAPOriginalTransactionId(1000L)
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(appleSubscriber)

    val googleSubscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = null,
      type = InAppPaymentSubscriberRecord.Type.BACKUP,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.GOOGLE_PLAY_BILLING,
      iapSubscriptionId = IAPSubscriptionId.GooglePlayBillingPurchaseToken("testToken")
    )

    ZonaRosaDatabase.inAppPaymentSubscribers.insertOrReplace(googleSubscriber)

    val subscriberCount = ZonaRosaDatabase.inAppPaymentSubscribers.readableDatabase.count()
      .from(InAppPaymentSubscriberTable.TABLE_NAME)
      .run()
      .readToSingleInt()

    assertThat(subscriberCount).isEqualTo(1)

    val subscriber = InAppPaymentsRepository.requireSubscriber(InAppPaymentSubscriberRecord.Type.BACKUP)
    assertThat(subscriber.iapSubscriptionId?.originalTransactionId).isNull()
    assertThat(subscriber.iapSubscriptionId?.purchaseToken).isEqualTo("testToken")
    assertThat(subscriber.subscriberId).isEqualTo(googleSubscriber.subscriberId)
  }
}
