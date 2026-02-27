package io.zonarosa.service.api.messages

import io.zonarosa.service.internal.push.Envelope
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage

/**
 * Represents an envelope off the wire, paired with the metadata needed to process it.
 */
class EnvelopeResponse(
  val envelope: Envelope,
  val serverDeliveredTimestamp: Long,
  val websocketRequest: WebSocketRequestMessage
)
