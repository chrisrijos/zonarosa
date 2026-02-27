/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.zonarosa.core.util.Base64

/**
 * JSON serializer to encode a ByteArray as a base64 string without padding.
 */
class ByteArraySerializerBase64NoPadding : JsonSerializer<ByteArray>() {
  override fun serialize(value: ByteArray, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeString(Base64.encodeWithoutPadding(value))
  }
}
