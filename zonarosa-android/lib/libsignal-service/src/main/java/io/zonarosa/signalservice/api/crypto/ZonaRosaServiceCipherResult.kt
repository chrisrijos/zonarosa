package io.zonarosa.service.api.crypto

import io.zonarosa.service.internal.push.Content

/**
 * Represents the output of decrypting a [ZonaRosaServiceProtos.Envelope] via [ZonaRosaServiceCipher.decrypt]
 *
 * @param content The [ZonaRosaServiceProtos.Content] that was decrypted from the envelope.
 * @param metadata The decrypted metadata of the envelope. Represents sender information that may have
 *                 been encrypted with sealed sender.
 */
data class ZonaRosaServiceCipherResult(
  val content: Content,
  val metadata: EnvelopeMetadata
)
