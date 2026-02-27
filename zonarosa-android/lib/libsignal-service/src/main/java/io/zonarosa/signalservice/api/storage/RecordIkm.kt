/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import io.zonarosa.core.models.storageservice.StorageItemKey
import io.zonarosa.service.api.crypto.Crypto
import io.zonarosa.service.internal.storage.protos.ManifestRecord
import io.zonarosa.service.internal.storage.protos.StorageItem
import io.zonarosa.service.internal.util.Util

/**
 * A wrapper around a [ByteArray], just so the recordIkm is strongly typed.
 * The recordIkm comes from [ManifestRecord.recordIkm], and is used to encrypt [StorageItem.value_].
 */
@JvmInline
value class RecordIkm(val value: ByteArray) {

  companion object {
    fun generate(): RecordIkm {
      return RecordIkm(Util.getSecretBytes(32))
    }
  }

  fun deriveStorageItemKey(rawId: ByteArray): StorageItemKey {
    val key = Crypto.hkdf(
      inputKeyMaterial = this.value,
      info = "20240801_ZONAROSA_STORAGE_SERVICE_ITEM_".toByteArray(Charsets.UTF_8) + rawId,
      outputLength = 32
    )

    return StorageItemKey(key)
  }
}
