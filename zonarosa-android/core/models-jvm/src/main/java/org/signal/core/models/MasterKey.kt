/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.core.models

import io.zonarosa.core.models.storageservice.StorageKey
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.CryptoUtil
import io.zonarosa.core.util.Hex
import java.security.SecureRandom

class MasterKey(masterKey: ByteArray) {
  private val masterKey: ByteArray

  companion object {
    private const val LENGTH = 32

    fun createNew(secureRandom: SecureRandom): MasterKey {
      val key = ByteArray(LENGTH)
      secureRandom.nextBytes(key)
      return MasterKey(key)
    }
  }

  init {
    check(masterKey.size == LENGTH) { "Master key must be $LENGTH bytes long (actualSize: ${masterKey.size})" }
    this.masterKey = masterKey
  }

  fun deriveRegistrationLock(): String {
    return Hex.toStringCondensed(derive("Registration Lock"))
  }

  fun deriveRegistrationRecoveryPassword(): String {
    return Base64.encodeWithPadding(derive("Registration Recovery")!!)
  }

  fun deriveStorageServiceKey(): StorageKey {
    return StorageKey(derive("Storage Service Encryption")!!)
  }

  fun deriveLoggingKey(): ByteArray? {
    return derive("Logging Key")
  }

  private fun derive(keyName: String): ByteArray? {
    return CryptoUtil.hmacSha256(masterKey, keyName.toByteArray(Charsets.UTF_8))
  }

  fun serialize(): ByteArray {
    return masterKey.clone()
  }

  override fun equals(o: Any?): Boolean {
    if (o == null || o.javaClass != javaClass) return false

    return (o as MasterKey).masterKey.contentEquals(masterKey)
  }

  override fun hashCode(): Int {
    return masterKey.contentHashCode()
  }

  override fun toString(): String {
    return "MasterKey(xxx)"
  }
}
