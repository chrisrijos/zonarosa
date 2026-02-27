package io.zonarosa.messenger.components.settings.app.subscription.donate.gateway

import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.messenger.database.InAppPaymentTable

sealed interface GatewaySelectorState {
  data object Loading : GatewaySelectorState

  data class Ready(
    val gatewayOrderStrategy: GatewayOrderStrategy,
    val inAppPayment: InAppPaymentTable.InAppPayment,
    val isGooglePayAvailable: Boolean = false,
    val isPayPalAvailable: Boolean = false,
    val isCreditCardAvailable: Boolean = false,
    val isSEPADebitAvailable: Boolean = false,
    val isIDEALAvailable: Boolean = false,
    val sepaEuroMaximum: FiatMoney? = null
  ) : GatewaySelectorState
}
