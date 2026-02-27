package io.zonarosa.messenger.components.settings.app.subscription.receipts.detail

import io.zonarosa.messenger.database.model.InAppPaymentReceiptRecord

data class DonationReceiptDetailState(
  val inAppPaymentReceiptRecord: InAppPaymentReceiptRecord? = null
)
