/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.crypto

import io.zonarosa.libzonarosa.protocol.kdf.HKDF

/**
 * A collection of cryptographic functions in the same namespace for easy access.
 */
object Crypto {

  fun hkdf(inputKeyMaterial: ByteArray, info: ByteArray, outputLength: Int, salt: ByteArray? = null): ByteArray {
    return HKDF.deriveSecrets(inputKeyMaterial, salt ?: byteArrayOf(), info, outputLength)
  }
}
