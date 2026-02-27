package io.zonarosa.messenger.messages.protocol

import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyRecord
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore
import io.zonarosa.service.api.ZonaRosaServiceSenderKeyStore
import io.zonarosa.service.api.push.DistributionId
import java.util.UUID

/**
 * An in-memory sender key store that is intended to be used temporarily while decrypting messages.
 */
class BufferedSenderKeyStore : ZonaRosaServiceSenderKeyStore {

  private val store: MutableMap<StoreKey, SenderKeyRecord> = HashMap()

  /** All of the keys that have been created or updated during operation. */
  private val updatedKeys: MutableMap<StoreKey, SenderKeyRecord> = mutableMapOf()

  /** All of the distributionId's whose sharing has been cleared during operation. */
  private val clearSharedWith: MutableSet<ZonaRosaProtocolAddress> = mutableSetOf()

  override fun storeSenderKey(sender: ZonaRosaProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
    val key = StoreKey(sender, distributionId)
    store[key] = record
    updatedKeys[key] = record
  }

  override fun loadSenderKey(sender: ZonaRosaProtocolAddress, distributionId: UUID): SenderKeyRecord? {
    val cached: SenderKeyRecord? = store[StoreKey(sender, distributionId)]

    return if (cached != null) {
      cached
    } else {
      val fromDatabase: SenderKeyRecord? = ZonaRosaDatabase.senderKeys.load(sender, distributionId.toDistributionId())

      if (fromDatabase != null) {
        store[StoreKey(sender, distributionId)] = fromDatabase
      }

      return fromDatabase
    }
  }

  override fun clearSenderKeySharedWith(addresses: MutableCollection<ZonaRosaProtocolAddress>) {
    clearSharedWith.addAll(addresses)
  }

  override fun getSenderKeySharedWith(distributionId: DistributionId?): MutableSet<ZonaRosaProtocolAddress> {
    error("Should not happen during the intended usage pattern of this class")
  }

  override fun markSenderKeySharedWith(distributionId: DistributionId?, addresses: MutableCollection<ZonaRosaProtocolAddress>?) {
    error("Should not happen during the intended usage pattern of this class")
  }

  fun flushToDisk(persistentStore: ZonaRosaServiceAccountDataStore) {
    for ((key, record) in updatedKeys) {
      persistentStore.storeSenderKey(key.address, key.distributionId, record)
    }

    if (clearSharedWith.isNotEmpty()) {
      persistentStore.clearSenderKeySharedWith(clearSharedWith)
      clearSharedWith.clear()
    }

    updatedKeys.clear()
  }

  private fun UUID.toDistributionId() = DistributionId.from(this)

  data class StoreKey(
    val address: ZonaRosaProtocolAddress,
    val distributionId: UUID
  )
}
