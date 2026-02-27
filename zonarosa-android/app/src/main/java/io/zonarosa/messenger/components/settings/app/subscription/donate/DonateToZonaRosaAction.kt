package io.zonarosa.messenger.components.settings.app.subscription.donate

import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.database.InAppPaymentTable

sealed class DonateToZonaRosaAction {
  data class DisplayCurrencySelectionDialog(val inAppPaymentType: InAppPaymentType, val supportedCurrencies: List<String>) : DonateToZonaRosaAction()
  data class DisplayGatewaySelectorDialog(val inAppPayment: InAppPaymentTable.InAppPayment) : DonateToZonaRosaAction()
  data object CancelSubscription : DonateToZonaRosaAction()
  data class UpdateSubscription(val inAppPayment: InAppPaymentTable.InAppPayment, val isLongRunning: Boolean) : DonateToZonaRosaAction()
}
