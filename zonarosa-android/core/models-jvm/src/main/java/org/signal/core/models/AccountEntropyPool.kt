/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models

import io.zonarosa.core.models.backup.MessageBackupKey

private typealias LibZonaRosaAccountEntropyPool = io.zonarosa.libzonarosa.messagebackup.AccountEntropyPool

/**
 * The Root of All Entropy. You can use this to derive the [io.zonarosa.service.api.kbs.MasterKey] or [io.zonarosa.service.api.backup.MessageBackupKey].
 */
class AccountEntropyPool(value: String) {

  val value = value.lowercase()
  val displayValue = value.uppercase()

  companion object {
    private val INVALID_CHARACTERS = Regex("[^0-9a-zA-Z]")
    const val LENGTH = 64

    fun generate(): AccountEntropyPool {
      return AccountEntropyPool(LibZonaRosaAccountEntropyPool.generate())
    }

    fun parseOrNull(input: String): AccountEntropyPool? {
      val stripped = removeIllegalCharacters(input)
      if (stripped.length != LENGTH) {
        return null
      }

      return AccountEntropyPool(stripped)
    }

    fun isFullyValid(input: String): Boolean {
      return LibZonaRosaAccountEntropyPool.isValid(input)
    }

    fun removeIllegalCharacters(input: String): String {
      return input.replace(INVALID_CHARACTERS, "")
    }
  }

  fun deriveMasterKey(): MasterKey {
    return MasterKey(LibZonaRosaAccountEntropyPool.deriveSvrKey(value))
  }

  fun deriveMessageBackupKey(): MessageBackupKey {
    val libZonaRosaBackupKey = LibZonaRosaAccountEntropyPool.deriveBackupKey(value)
    return MessageBackupKey(libZonaRosaBackupKey.serialize())
  }
}
