/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.backup

import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex

/**
 * Safe typing around a mediaId, which is a 15-byte array.
 */
@JvmInline
value class MediaId(val value: ByteArray) {

  constructor(mediaId: String) : this(Base64.decode(mediaId))

  init {
    require(value.size == 15) { "MediaId must be 15 bytes!" }
  }

  /** Encode media-id for use in a URL/request */
  fun encode(): String {
    return Base64.encodeUrlSafeWithPadding(value)
  }

  override fun toString(): String {
    return "MediaId::${Hex.toStringCondensed(value)}"
  }
}
