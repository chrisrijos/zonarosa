/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey

/**
 * Represents the request body when setting the archive public key.
 */
class ArchiveSetPublicKeyRequest(
  @JsonProperty
  @JsonSerialize(using = PublicKeySerializer::class)
  val backupIdPublicKey: ECPublicKey
) {
  class PublicKeySerializer : JsonSerializer<ECPublicKey>() {
    override fun serialize(value: ECPublicKey, gen: JsonGenerator, serializers: SerializerProvider) {
      gen.writeString(Base64.encodeWithPadding(value.serialize()))
    }
  }
}
