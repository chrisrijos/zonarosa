/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import java.nio.ByteBuffer

/**
 * Converts the long into [ByteArray].
 */
fun Long.toByteArray(): ByteArray {
  return ByteBuffer
    .allocate(Long.SIZE_BYTES)
    .putLong(this)
    .array()
}
