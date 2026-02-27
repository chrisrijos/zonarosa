/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.attachments

import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * Combined key used to encrypt/decrypt attachments for local backups.
 */
@JvmInline
value class LocalBackupKey(val key: ByteArray) {
  fun toByteString(): ByteString {
    return key.toByteString()
  }
}
