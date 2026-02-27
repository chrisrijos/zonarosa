package io.zonarosa.messenger.messages

import io.zonarosa.service.api.crypto.EnvelopeMetadata
import io.zonarosa.service.internal.push.Content
import io.zonarosa.service.internal.push.Envelope

data class TestMessage(
  val envelope: Envelope,
  val content: Content,
  val metadata: EnvelopeMetadata,
  val serverDeliveredTimestamp: Long
)
