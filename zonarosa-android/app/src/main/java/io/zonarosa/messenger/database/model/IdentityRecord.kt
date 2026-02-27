package io.zonarosa.messenger.database.model

import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.recipients.RecipientId

data class IdentityRecord(
  val recipientId: RecipientId,
  val identityKey: IdentityKey,
  val verifiedStatus: IdentityTable.VerifiedStatus,
  @get:JvmName("isFirstUse")
  val firstUse: Boolean,
  val timestamp: Long,
  @get:JvmName("isApprovedNonBlocking")
  val nonblockingApproval: Boolean
)
