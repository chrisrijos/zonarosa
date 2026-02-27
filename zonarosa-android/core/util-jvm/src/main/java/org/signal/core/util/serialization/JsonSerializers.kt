/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.libzonarosa.protocol.kem.KEMPublicKey

class ByteArrayToBase64Serializer() : KSerializer<ByteArray> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): ByteArray {
    return Base64.decode(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: ByteArray) {
    encoder.encodeString(Base64.encodeWithPadding(value))
  }
}

class KEMPublicKeyToBase64Serializer() : KSerializer<KEMPublicKey> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KEMPublicKey", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): KEMPublicKey {
    return KEMPublicKey(Base64.decode(decoder.decodeString()))
  }

  override fun serialize(encoder: Encoder, value: KEMPublicKey) {
    encoder.encodeString(Base64.encodeWithPadding(value.serialize()))
  }
}

class ECPublicKeyToBase64Serializer() : KSerializer<ECPublicKey> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ECPublicKey", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): ECPublicKey {
    return ECPublicKey(Base64.decode(decoder.decodeString()))
  }

  override fun serialize(encoder: Encoder, value: ECPublicKey) {
    encoder.encodeString(Base64.encodeWithPadding(value.serialize()))
  }
}
