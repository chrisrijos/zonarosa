package io.zonarosa.messenger.database.model

import io.zonarosa.messenger.recipients.RecipientId

/** A model for [io.zonarosa.messenger.database.PendingRetryReceiptTable] */
data class PendingRetryReceiptModel(
  val id: Long,
  val author: RecipientId,
  val authorDevice: Int,
  val sentTimestamp: Long,
  val receivedTimestamp: Long,
  val threadId: Long
)
