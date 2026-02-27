package io.zonarosa.messenger.components.settings.app.subscription.donate

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.testing.InAppPaymentsRule
import io.zonarosa.messenger.testing.RxTestSchedulerRule
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.messenger.testing.actions.RecyclerViewScrollToBottomAction
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.subscriptions.ActiveSubscription
import io.zonarosa.service.api.subscriptions.SubscriberId
import java.math.BigDecimal
import java.util.Currency
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@Suppress("ClassName")
@RunWith(AndroidJUnit4::class)
class CheckoutFlowActivityTest__RecurringDonations {
  @get:Rule
  val harness = ZonaRosaActivityRule(othersCount = 10)

  @get:Rule
  val iapRule = InAppPaymentsRule()

  @get:Rule
  val rxRule = RxTestSchedulerRule()

  private val intent = CheckoutFlowActivity.createIntent(InstrumentationRegistry.getInstrumentation().targetContext, InAppPaymentType.RECURRING_DONATION)

  @Test
  fun givenRecurringDonations_whenILoadScreen_thenIExpectMonthlySelected() {
    ActivityScenario.launch<CheckoutFlowActivity>(intent)
    onView(withId(R.id.monthly)).check(matches(isSelected()))
  }

  @Test
  fun givenNoCurrentDonation_whenILoadScreen_thenIExpectContinueButton() {
    ActivityScenario.launch<CheckoutFlowActivity>(intent)
    onView(withId(R.id.recycler)).perform(RecyclerViewScrollToBottomAction)
    onView(withText("Continue")).check(matches(isDisplayed()))
  }

  @Test
  fun givenACurrentDonation_whenILoadScreen_thenIExpectUpgradeButton() {
    initialiseActiveSubscription()

    ActivityScenario.launch<CheckoutFlowActivity>(intent)

    rxRule.defaultTestScheduler.triggerActions()

    onView(withId(R.id.recycler)).perform(RecyclerViewScrollToBottomAction)
    onView(withText(R.string.SubscribeFragment__update_subscription)).check(matches(isDisplayed()))
    onView(withText(R.string.SubscribeFragment__cancel_subscription)).check(matches(isDisplayed()))
  }

  @Test
  fun givenACurrentDonation_whenIPressCancel_thenIExpectCancellationDialog() {
    initialiseActiveSubscription()

    ActivityScenario.launch<CheckoutFlowActivity>(intent)

    rxRule.defaultTestScheduler.triggerActions()

    onView(withId(R.id.recycler)).perform(RecyclerViewScrollToBottomAction)
    onView(withText(R.string.SubscribeFragment__cancel_subscription)).check(matches(isDisplayed()))
    onView(withText(R.string.SubscribeFragment__cancel_subscription)).perform(ViewActions.click())
    onView(withText(R.string.SubscribeFragment__confirm_cancellation)).check(matches(isDisplayed()))
    onView(withText(R.string.SubscribeFragment__confirm)).perform(ViewActions.click())
  }

  @Test
  fun givenAPendingRecurringDonation_whenILoadScreen_thenIExpectDisabledUpgradeButton() {
    initialisePendingSubscription()

    ActivityScenario.launch<CheckoutFlowActivity>(intent)

    rxRule.defaultTestScheduler.triggerActions()

    onView(withId(R.id.recycler)).perform(RecyclerViewScrollToBottomAction)
    onView(withText(R.string.SubscribeFragment__update_subscription)).check(matches(isDisplayed()))
    onView(withText(R.string.SubscribeFragment__update_subscription)).check(matches(isNotEnabled()))
  }

  private fun initialiseActiveSubscription() {
    val currency = Currency.getInstance("USD")
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = currency,
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = null
    )

    InAppPaymentsRepository.setSubscriber(subscriber)
    ZonaRosaStore.inAppPayments.setRecurringDonationCurrency(currency)

    AppDependencies.donationsApi.apply {
      every { getSubscription(subscriber.subscriberId) } returns NetworkResult.Success(
        ActiveSubscription(
          ActiveSubscription.Subscription(
            200,
            currency.currencyCode,
            BigDecimal.ONE,
            System.currentTimeMillis().milliseconds.inWholeSeconds + 30.days.inWholeSeconds,
            true,
            System.currentTimeMillis().milliseconds.inWholeSeconds + 30.days.inWholeSeconds,
            false,
            "active",
            "STRIPE",
            "CARD",
            false
          ),
          null
        )
      )

      every { deleteSubscription(subscriber.subscriberId) } returns NetworkResult.Success(Unit)
    }
  }

  private fun initialisePendingSubscription() {
    val currency = Currency.getInstance("USD")
    val subscriber = InAppPaymentSubscriberRecord(
      subscriberId = SubscriberId.generate(),
      currency = currency,
      type = InAppPaymentSubscriberRecord.Type.DONATION,
      requiresCancel = false,
      paymentMethodType = InAppPaymentData.PaymentMethodType.CARD,
      iapSubscriptionId = null
    )

    InAppPaymentsRepository.setSubscriber(subscriber)
    ZonaRosaStore.inAppPayments.setRecurringDonationCurrency(currency)

    AppDependencies.donationsApi.apply {
      every { getSubscription(subscriber.subscriberId) } returns NetworkResult.Success(
        ActiveSubscription(
          ActiveSubscription.Subscription(
            200,
            currency.currencyCode,
            BigDecimal.ONE,
            System.currentTimeMillis().milliseconds.inWholeSeconds + 30.days.inWholeSeconds,
            false,
            System.currentTimeMillis().milliseconds.inWholeSeconds + 30.days.inWholeSeconds,
            false,
            "incomplete",
            "STRIPE",
            "CARD",
            false
          ),
          null
        )
      )
    }
  }
}
