package io.zonarosa.messenger.components.settings.app.subscription

import androidx.annotation.WorkerThread
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.PaymentSourceType
import io.zonarosa.messenger.components.settings.app.subscription.donate.paypal.PayPalConfirmationResult
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.api.services.DonationsService
import io.zonarosa.service.api.subscriptions.PayPalConfirmPaymentIntentResponse
import io.zonarosa.service.api.subscriptions.PayPalCreatePaymentIntentResponse
import io.zonarosa.service.api.subscriptions.PayPalCreatePaymentMethodResponse
import java.util.Locale

/**
 * Repository that deals directly with PayPal API calls. Since we don't interact with the PayPal APIs directly (yet)
 * we can do everything here in one place.
 */
class PayPalRepository(private val donationsService: DonationsService) {

  companion object {
    const val ONE_TIME_RETURN_URL = "https://zonarosadonations.org/return/onetime"
    const val MONTHLY_RETURN_URL = "https://zonarosadonations.org/return/monthly"
    const val CANCEL_URL = "https://zonarosadonations.org/cancel"

    private val TAG = Log.tag(PayPalRepository::class.java)
  }

  /**
   * Creates a one-time payment intent that the user can use to make a donation.
   */
  @WorkerThread
  fun createOneTimePaymentIntent(
    amount: FiatMoney,
    badgeRecipient: RecipientId,
    badgeLevel: Long
  ): PayPalCreatePaymentIntentResponse {
    return try {
      donationsService.createPayPalOneTimePaymentIntent(
        Locale.getDefault(),
        amount.currency.currencyCode,
        amount.minimumUnitPrecisionString,
        badgeLevel,
        ONE_TIME_RETURN_URL,
        CANCEL_URL
      ).resultOrThrow
    } catch (e: Exception) {
      throw OneTimeInAppPaymentRepository.handleCreatePaymentIntentErrorSync(e, badgeRecipient, PaymentSourceType.PayPal)
    }
  }

  /**
   * Confirms a one-time payment via the ZonaRosa Service to complete a donation.
   */
  @WorkerThread
  fun confirmOneTimePaymentIntent(
    amount: FiatMoney,
    badgeLevel: Long,
    paypalConfirmationResult: PayPalConfirmationResult
  ): PayPalConfirmPaymentIntentResponse {
    Log.d(TAG, "Confirming one-time payment intent...", true)
    return donationsService.confirmPayPalOneTimePaymentIntent(
      amount.currency.currencyCode,
      amount.minimumUnitPrecisionString,
      badgeLevel,
      paypalConfirmationResult.payerId,
      paypalConfirmationResult.paymentId,
      paypalConfirmationResult.paymentToken
    ).resultOrThrow
  }

  /**
   * Creates the PaymentMethod via the ZonaRosa Service. Note that if the operation fails with a 409,
   * it means that the PaymentMethod is already tied to a Stripe account. We can retry in this
   * situation by simply deleting the old subscriber id on the service and replacing it.
   */
  @WorkerThread
  fun createPaymentMethod(subscriberType: InAppPaymentSubscriberRecord.Type, retryOn409: Boolean = true): PayPalCreatePaymentMethodResponse {
    val response = donationsService.createPayPalPaymentMethod(
      Locale.getDefault(),
      InAppPaymentsRepository.requireSubscriber(subscriberType).subscriberId,
      MONTHLY_RETURN_URL,
      CANCEL_URL
    )

    return if (retryOn409 && response.status == 409) {
      RecurringInAppPaymentRepository.rotateSubscriberIdSync(subscriberType)
      createPaymentMethod(subscriberType, retryOn409 = false)
    } else {
      response.resultOrThrow
    }
  }

  /**
   * Sets the default payment method via the ZonaRosa Service.
   */
  @WorkerThread
  fun setDefaultPaymentMethod(subscriberType: InAppPaymentSubscriberRecord.Type, paymentMethodId: String) {
    val subscriber = InAppPaymentsRepository.requireSubscriber(subscriberType)

    Log.d(TAG, "Setting default payment method...", true)
    donationsService.setDefaultPayPalPaymentMethod(
      subscriber.subscriberId,
      paymentMethodId
    ).resultOrThrow

    Log.d(TAG, "Set default payment method.", true)
    Log.d(TAG, "Storing the subscription payment source type locally.", true)

    ZonaRosaDatabase.inAppPaymentSubscribers.setPaymentMethod(
      subscriber.subscriberId,
      InAppPaymentData.PaymentMethodType.PAYPAL
    )
  }
}
