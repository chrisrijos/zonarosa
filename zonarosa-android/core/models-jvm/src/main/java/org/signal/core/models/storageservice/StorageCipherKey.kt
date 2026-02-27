/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.storageservice

interface StorageCipherKey {
  fun serialize(): ByteArray
}
