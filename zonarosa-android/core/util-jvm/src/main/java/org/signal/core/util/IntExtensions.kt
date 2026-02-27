/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import java.nio.ByteBuffer

/**
 * Converts the integer into [ByteArray].
 */
fun Int.toByteArray(): ByteArray {
  return ByteBuffer
    .allocate(Int.SIZE_BYTES)
    .putInt(this)
    .array()
}
