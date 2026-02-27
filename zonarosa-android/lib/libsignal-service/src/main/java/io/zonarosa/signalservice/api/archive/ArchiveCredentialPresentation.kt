/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import io.zonarosa.core.util.Base64

/**
 * Acts as credentials for various archive operations.
 */
class ArchiveCredentialPresentation(
  val presentation: ByteArray,
  val signedPresentation: ByteArray
) {
  fun toHeaders(): MutableMap<String, String> {
    return mutableMapOf(
      "X-ZonaRosa-ZK-Auth" to Base64.encodeWithPadding(presentation),
      "X-ZonaRosa-ZK-Auth-Signature" to Base64.encodeWithPadding(signedPresentation)
    )
  }
}
