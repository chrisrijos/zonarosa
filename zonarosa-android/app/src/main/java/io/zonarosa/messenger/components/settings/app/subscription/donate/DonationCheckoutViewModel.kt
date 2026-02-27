package io.zonarosa.messenger.components.settings.app.subscription.donate

import androidx.lifecycle.ViewModel
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.service.api.util.Preconditions

/**
 * State holder for the checkout flow when utilizing Google Pay.
 */
class DonationCheckoutViewModel : ViewModel() {

  companion object {
    private val TAG = Log.tag(DonationCheckoutViewModel::class.java)
  }

  private var inAppPayment: InAppPaymentTable.InAppPayment? = null

  fun provideGatewayRequestForGooglePay(inAppPayment: InAppPaymentTable.InAppPayment) {
    Log.d(TAG, "Provided with a gateway request.")
    Preconditions.checkState(this.inAppPayment == null)
    this.inAppPayment = inAppPayment
  }

  fun consumeGatewayRequestForGooglePay(): InAppPaymentTable.InAppPayment? {
    val request = inAppPayment
    inAppPayment = null
    return request
  }
}
