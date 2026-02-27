package io.zonarosa.messenger.messages.protocol

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore
import io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore.IdentityChange
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore

/**
 * An in-memory identity key store that is intended to be used temporarily while decrypting messages.
 */
class BufferedIdentityKeyStore(
  private val selfServiceId: ServiceId,
  private val selfIdentityKeyPair: IdentityKeyPair,
  private val selfRegistrationId: Int
) : IdentityKeyStore {

  private val store: MutableMap<ZonaRosaProtocolAddress, IdentityKey> = HashMap()

  /** All of the keys that have been created or updated during operation. */
  private val updatedKeys: MutableMap<ZonaRosaProtocolAddress, IdentityKey> = mutableMapOf()

  override fun getIdentityKeyPair(): IdentityKeyPair {
    return selfIdentityKeyPair
  }

  override fun getLocalRegistrationId(): Int {
    return selfRegistrationId
  }

  override fun saveIdentity(address: ZonaRosaProtocolAddress, identityKey: IdentityKey): IdentityChange {
    val existing: IdentityKey? = getIdentity(address)

    store[address] = identityKey

    return if (identityKey != existing) {
      updatedKeys[address] = identityKey
      IdentityChange.REPLACED_EXISTING
    } else {
      IdentityChange.NEW_OR_UNCHANGED
    }
  }

  override fun isTrustedIdentity(address: ZonaRosaProtocolAddress, identityKey: IdentityKey, direction: IdentityKeyStore.Direction): Boolean {
    if (address.name == selfServiceId.toString()) {
      return identityKey == selfIdentityKeyPair.publicKey
    }

    return when (direction) {
      IdentityKeyStore.Direction.RECEIVING -> true
      IdentityKeyStore.Direction.SENDING -> error("Should not happen during the intended usage pattern of this class")
      else -> error("Unknown direction: $direction")
    }
  }

  override fun getIdentity(address: ZonaRosaProtocolAddress): IdentityKey? {
    val cached = store[address]

    return if (cached != null) {
      cached
    } else {
      val fromDatabase = ZonaRosaDatabase.identities.getIdentityStoreRecord(address.name)
      if (fromDatabase != null) {
        store[address] = fromDatabase.identityKey
      }

      fromDatabase?.identityKey
    }
  }

  fun flushToDisk(persistentStore: ZonaRosaServiceAccountDataStore) {
    for ((address, identityKey) in updatedKeys) {
      persistentStore.saveIdentity(address, identityKey)
    }

    updatedKeys.clear()
  }
}
