package io.zonarosa.service.api.messages

data class ZonaRosaServiceEditMessage(
  val targetSentTimestamp: Long,
  val dataMessage: ZonaRosaServiceDataMessage
)
