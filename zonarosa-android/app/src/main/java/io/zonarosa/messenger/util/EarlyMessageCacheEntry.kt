package io.zonarosa.messenger.util

import io.zonarosa.service.api.crypto.EnvelopeMetadata
import io.zonarosa.service.internal.push.Content
import io.zonarosa.service.internal.push.Envelope

/**
 * The tuple of information needed to process a message. Used to in [EarlyMessageCache]
 * to store potentially out-of-order messages.
 */
data class EarlyMessageCacheEntry(
  val envelope: Envelope,
  val content: Content,
  val metadata: EnvelopeMetadata,
  val serverDeliveredTimestamp: Long
)
