/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.backup

import io.zonarosa.core.util.Base64
import java.security.MessageDigest

/**
 * Safe typing around a backupId, which is a 16-byte array.
 */
@JvmInline
value class BackupId(val value: ByteArray) {

  init {
    require(value.size == 16) { "BackupId must be 16 bytes!" }
  }

  /** Encode backup-id for use in a URL/request */
  fun encode(): String {
    return Base64.encodeUrlSafeWithPadding(MessageDigest.getInstance("SHA-256").digest(value).copyOfRange(0, 16))
  }
}
