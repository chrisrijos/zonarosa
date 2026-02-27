package io.zonarosa.messenger.database.model

import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.api.crypto.ContentHint
import io.zonarosa.service.internal.push.Content

/**
 * Model class for reading from the [io.zonarosa.messenger.database.MessageSendLogTables].
 */
data class MessageLogEntry(
  val recipientId: RecipientId,
  val dateSent: Long,
  val content: Content,
  val contentHint: ContentHint,
  @get:JvmName("isUrgent")
  val urgent: Boolean,
  val relatedMessages: List<MessageId>
) {
  val hasRelatedMessage: Boolean
    @JvmName("hasRelatedMessage")
    get() = relatedMessages.isNotEmpty()
}
