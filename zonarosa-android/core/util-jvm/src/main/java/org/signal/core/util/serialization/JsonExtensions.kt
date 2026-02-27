/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.serialization

import kotlinx.serialization.json.Json

/**
 * Just like [Json.decodeFromString], except in the case of a parsing failure, this will return null instead of throwing.
 */
inline fun <reified T> Json.decodeFromStringOrNull(string: String): T? {
  return runCatching { decodeFromString<T>(string) }.getOrNull()
}
