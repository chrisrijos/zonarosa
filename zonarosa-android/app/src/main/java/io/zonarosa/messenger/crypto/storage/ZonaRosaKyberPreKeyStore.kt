/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.crypto.storage

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyStore
import io.zonarosa.messenger.crypto.ReentrantSessionLock
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.service.api.ZonaRosaServiceKyberPreKeyStore
import kotlin.jvm.Throws

/**
 * An implementation of the [KyberPreKeyStore] that stores entries in [io.zonarosa.messenger.database.KyberPreKeyTable].
 */
class ZonaRosaKyberPreKeyStore(private val selfServiceId: ServiceId) : ZonaRosaServiceKyberPreKeyStore {

  @Throws(InvalidKeyIdException::class)
  override fun loadKyberPreKey(kyberPreKeyId: Int): KyberPreKeyRecord {
    ReentrantSessionLock.INSTANCE.acquire().use {
      return ZonaRosaDatabase.kyberPreKeys.get(selfServiceId, kyberPreKeyId)?.record ?: throw InvalidKeyIdException("Missing kyber prekey with ID: $kyberPreKeyId")
    }
  }

  override fun loadKyberPreKeys(): List<KyberPreKeyRecord> {
    ReentrantSessionLock.INSTANCE.acquire().use {
      return ZonaRosaDatabase.kyberPreKeys.getAll(selfServiceId).map { it.record }
    }
  }

  override fun loadLastResortKyberPreKeys(): List<KyberPreKeyRecord> {
    ReentrantSessionLock.INSTANCE.acquire().use {
      return ZonaRosaDatabase.kyberPreKeys.getAllLastResort(selfServiceId).map { it.record }
    }
  }

  override fun storeKyberPreKey(kyberPreKeyId: Int, record: KyberPreKeyRecord) {
    ReentrantSessionLock.INSTANCE.acquire().use {
      return ZonaRosaDatabase.kyberPreKeys.insert(selfServiceId, kyberPreKeyId, record, false)
    }
  }

  override fun storeLastResortKyberPreKey(kyberPreKeyId: Int, kyberPreKeyRecord: KyberPreKeyRecord) {
    ReentrantSessionLock.INSTANCE.acquire().use {
      return ZonaRosaDatabase.kyberPreKeys.insert(selfServiceId, kyberPreKeyId, kyberPreKeyRecord, true)
    }
  }

  override fun containsKyberPreKey(kyberPreKeyId: Int): Boolean {
    ReentrantSessionLock.INSTANCE.acquire().use {
      return ZonaRosaDatabase.kyberPreKeys.contains(selfServiceId, kyberPreKeyId)
    }
  }

  override fun markKyberPreKeyUsed(kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey) {
    ReentrantSessionLock.INSTANCE.acquire().use {
      ZonaRosaDatabase.kyberPreKeys.handleMarkKyberPreKeyUsed(selfServiceId, kyberPreKeyId, signedPreKeyId, baseKey)
    }
  }

  override fun removeKyberPreKey(kyberPreKeyId: Int) {
    ReentrantSessionLock.INSTANCE.acquire().use {
      ZonaRosaDatabase.kyberPreKeys.delete(selfServiceId, kyberPreKeyId)
    }
  }

  override fun markAllOneTimeKyberPreKeysStaleIfNecessary(staleTime: Long) {
    ReentrantSessionLock.INSTANCE.acquire().use {
      ZonaRosaDatabase.kyberPreKeys.markAllStaleIfNecessary(selfServiceId, staleTime)
    }
  }

  override fun deleteAllStaleOneTimeKyberPreKeys(threshold: Long, minCount: Int) {
    ReentrantSessionLock.INSTANCE.acquire().use {
      ZonaRosaDatabase.kyberPreKeys.deleteAllStaleBefore(selfServiceId, threshold, minCount)
    }
  }
}
