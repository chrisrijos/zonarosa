/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import io.zonarosa.core.util.Base64

/**
 * Deserializes any valid base64 (regardless of padding or url-safety) into a ByteArray.
 */
class ByteArrayDeserializerBase64 : JsonDeserializer<ByteArray>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ByteArray {
    return Base64.decode(p.valueAsString)
  }
}
