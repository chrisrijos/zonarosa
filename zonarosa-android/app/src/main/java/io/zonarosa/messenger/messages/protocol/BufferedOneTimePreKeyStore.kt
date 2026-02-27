package io.zonarosa.messenger.messages.protocol

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.PreKeyStore
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore

/**
 * An in-memory one-time prekey store that is intended to be used temporarily while decrypting messages.
 */
class BufferedOneTimePreKeyStore(private val selfServiceId: ServiceId) : PreKeyStore {

  /** Our in-memory cache of one-time prekeys. */
  private val store: MutableMap<Int, PreKeyRecord> = HashMap()

  /** The one-time prekeys that have been marked as removed  */
  private val removed: MutableList<Int> = mutableListOf()

  @kotlin.jvm.Throws(InvalidKeyIdException::class)
  override fun loadPreKey(id: Int): PreKeyRecord {
    return store.computeIfAbsent(id) {
      ZonaRosaDatabase.oneTimePreKeys.get(selfServiceId, id) ?: throw InvalidKeyIdException("Missing one-time prekey with ID: $id")
    }
  }

  override fun storePreKey(id: Int, record: PreKeyRecord) {
    error("Should not happen during the intended usage pattern of this class")
  }

  override fun containsPreKey(id: Int): Boolean {
    loadPreKey(id)
    return store.containsKey(id)
  }

  override fun removePreKey(id: Int) {
    store.remove(id)
    removed += id
  }

  fun flushToDisk(persistentStore: ZonaRosaServiceAccountDataStore) {
    for (id in removed) {
      persistentStore.removePreKey(id)
    }

    removed.clear()
  }
}
