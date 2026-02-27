package io.zonarosa.messenger.safety

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.zonarosa.messenger.contacts.paged.ContactSearchKey
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.recipients.RecipientId

/**
 * Fragment argument for `SafetyNumberBottomSheetFragment`
 */
@Parcelize
data class SafetyNumberBottomSheetArgs(
  val untrustedRecipients: List<RecipientId>,
  val destinations: List<ContactSearchKey.RecipientSearchKey>,
  val messageId: MessageId? = null
) : Parcelable
