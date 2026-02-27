package io.zonarosa.messenger.components.settings.app.subscription.receipts.list

import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.database.model.InAppPaymentReceiptRecord

data class DonationReceiptBadge(
  val type: InAppPaymentReceiptRecord.Type,
  val level: Int,
  val badge: Badge
)
