package io.zonarosa.messenger.components.settings.app.subscription

import android.app.Application
import assertk.assertThat
import assertk.assertions.isNotEqualTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.testutil.MockAppDependenciesRule
import io.zonarosa.messenger.testutil.RxPluginsRule
import io.zonarosa.service.api.subscriptions.SubscriberId
import io.zonarosa.service.internal.EmptyResponse
import io.zonarosa.service.internal.ServiceResponse
import java.util.Currency

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class RecurringInAppPaymentRepositoryTest {

  @get:Rule
  val rxRule = RxPluginsRule()

  @get:Rule
  val appDependencies = MockAppDependenciesRule()

  @get:Rule
  val inAppPaymentsTestRule = InAppPaymentsTestRule()

  @Before
  fun setUp() {
    InAppPaymentsTestRule.mockLocalSubscriberAccess()

    mockkObject(ZonaRosaStore.Companion)
    every { ZonaRosaStore.Companion.inAppPayments } returns mockk {
      every { ZonaRosaStore.Companion.inAppPayments.getRecurringDonationCurrency() } returns Currency.getInstance("USD")
      every { ZonaRosaStore.Companion.inAppPayments.updateLocalStateForManualCancellation(any()) } returns Unit
      every { ZonaRosaStore.Companion.inAppPayments.updateLocalStateForLocalSubscribe(any()) } returns Unit
    }

    every { ZonaRosaDatabase.Companion.recipients } returns mockk {
      every { ZonaRosaDatabase.Companion.recipients.markNeedsSync(any<RecipientId>()) } returns Unit
    }

    mockkStatic(StorageSyncHelper::class)
    every { StorageSyncHelper.scheduleSyncForDataChange() } returns Unit

    every { AppDependencies.donationsService.putSubscription(any()) } returns ServiceResponse.forResult(EmptyResponse.INSTANCE, 200, "")
    every { AppDependencies.donationsService.updateSubscriptionLevel(any(), any(), any(), any(), any()) } returns ServiceResponse.forResult(EmptyResponse.INSTANCE, 200, "")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `when I getDonationsConfiguration then I expect a set of three Subscription objects`() {
    inAppPaymentsTestRule.initializeDonationsConfigurationMock()

    val testObserver = RecurringInAppPaymentRepository.getSubscriptions().test()
    rxRule.defaultScheduler.triggerActions()

    testObserver
      .assertComplete()
      .assertValueCount(1)
      .assertValue { it.size == 3 }
  }

  @Test
  fun `Given I do not need to rotate my subscriber id, when I ensureSubscriberId, then I use the same subscriber id`() {
    val initialSubscriber = createSubscriber()
    val ref = InAppPaymentsTestRule.mockLocalSubscriberAccess(initialSubscriber)

    RecurringInAppPaymentRepository.ensureSubscriberIdSync(
      subscriberType = InAppPaymentSubscriberRecord.Type.DONATION,
      isRotation = false
    )

    val newSubscriber = ref.get()

    assertThat(newSubscriber).isNotEqualTo(initialSubscriber)
  }

  @Test
  fun `Given I need to rotate my subscriber id, when I ensureSubscriberId, then I generate and set a new subscriber id`() {
    val initialSubscriber = createSubscriber()
    val ref = InAppPaymentsTestRule.mockLocalSubscriberAccess(initialSubscriber)

    RecurringInAppPaymentRepository.ensureSubscriberIdSync(
      subscriberType = InAppPaymentSubscriberRecord.Type.DONATION,
      isRotation = true
    )

    val newSubscriber = ref.get()

    assertThat(newSubscriber).isNotEqualTo(initialSubscriber)
  }

  @Test
  fun `Given no current subscriber, when I rotateSubscriberId, then I do not try to cancel subscription`() {
    val ref = InAppPaymentsTestRule.mockLocalSubscriberAccess()

    val testObserver = RecurringInAppPaymentRepository.rotateSubscriberId(InAppPaymentSubscriberRecord.Type.DONATION).test()

    rxRule.defaultScheduler.triggerActions()
    testObserver.assertComplete()

    assertThat(ref.get()).isNotEqualTo(null)
    verify(inverse = true) {
      AppDependencies.donationsService.cancelSubscription(any())
    }
  }

  @Test
  fun `Given current subscriber, when I rotateSubscriberId, then I do not try to cancel subscription`() {
    val initialSubscriber = createSubscriber()
    val ref = InAppPaymentsTestRule.mockLocalSubscriberAccess(initialSubscriber)

    val testObserver = RecurringInAppPaymentRepository.rotateSubscriberId(InAppPaymentSubscriberRecord.Type.DONATION).test()

    rxRule.defaultScheduler.triggerActions()
    testObserver.assertComplete()

    assertThat(ref.get()).isNotEqualTo(null)
    verify {
      AppDependencies.donationsService.cancelSubscription(any())
    }
  }

  private fun createSubscriber(): InAppPaymentSubscriberRecord {
    return InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = Currency.getInstance("USD"),
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = null
    )
  }
}
