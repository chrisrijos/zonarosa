package io.zonarosa.messenger.components.settings.app.subscription.receipts.list

import io.zonarosa.messenger.database.model.InAppPaymentReceiptRecord

data class DonationReceiptListPageState(
  val records: List<InAppPaymentReceiptRecord> = emptyList(),
  val isLoaded: Boolean = false
)
