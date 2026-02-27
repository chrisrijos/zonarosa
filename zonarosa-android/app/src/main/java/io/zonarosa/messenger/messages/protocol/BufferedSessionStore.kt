package io.zonarosa.messenger.messages.protocol

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.NoSessionException
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.libzonarosa.protocol.state.SessionRecord
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore
import io.zonarosa.service.api.ZonaRosaServiceSessionStore
import kotlin.jvm.Throws

/**
 * An in-memory session store that is intended to be used temporarily while decrypting messages.
 */
class BufferedSessionStore(private val selfServiceId: ServiceId) : ZonaRosaServiceSessionStore {

  private val store: MutableMap<ZonaRosaProtocolAddress, SessionRecord> = HashMap()

  /** All of the sessions that have been created or updated during operation. */
  private val updatedSessions: MutableMap<ZonaRosaProtocolAddress, SessionRecord> = mutableMapOf()

  /** All of the sessions that have deleted during operation. */
  private val deletedSessions: MutableSet<ZonaRosaProtocolAddress> = mutableSetOf()

  override fun loadSession(address: ZonaRosaProtocolAddress): SessionRecord {
    val session: SessionRecord = store[address]
      ?: ZonaRosaDatabase.sessions.load(selfServiceId, address)
      ?: SessionRecord()

    store[address] = session

    return session
  }

  @Throws(NoSessionException::class)
  override fun loadExistingSessions(addresses: MutableList<ZonaRosaProtocolAddress>): List<SessionRecord> {
    val found: MutableList<SessionRecord?> = ArrayList(addresses.size)
    val needsDatabaseLookup: MutableList<Pair<Int, ZonaRosaProtocolAddress>> = mutableListOf()

    addresses.forEachIndexed { index, address ->
      val cached: SessionRecord? = store[address]

      if (cached != null) {
        found[index] = cached
      } else {
        needsDatabaseLookup += (index to address)
      }
    }

    if (needsDatabaseLookup.isNotEmpty()) {
      val databaseRecords: List<SessionRecord?> = ZonaRosaDatabase.sessions.load(selfServiceId, needsDatabaseLookup.map { (_, address) -> address })
      needsDatabaseLookup.forEachIndexed { databaseLookupIndex, (addressIndex, _) ->
        found[addressIndex] = databaseRecords[databaseLookupIndex]
      }
    }

    val cachedAndLoaded = found.filterNotNull()

    if (cachedAndLoaded.size != addresses.size) {
      throw NoSessionException("Failed to find one or more sessions.")
    }

    return cachedAndLoaded
  }

  override fun storeSession(address: ZonaRosaProtocolAddress, record: SessionRecord) {
    store[address] = record
    updatedSessions[address] = record
  }

  override fun containsSession(address: ZonaRosaProtocolAddress): Boolean {
    return if (store.containsKey(address)) {
      true
    } else {
      val fromDatabase: SessionRecord? = ZonaRosaDatabase.sessions.load(selfServiceId, address)

      if (fromDatabase != null) {
        store[address] = fromDatabase
        return fromDatabase.hasSenderChain()
      } else {
        false
      }
    }
  }

  override fun deleteSession(address: ZonaRosaProtocolAddress) {
    store.remove(address)
    deletedSessions += address
  }

  override fun getSubDeviceSessions(name: String): MutableList<Int> {
    error("Should not happen during the intended usage pattern of this class")
  }

  override fun deleteAllSessions(name: String) {
    error("Should not happen during the intended usage pattern of this class")
  }

  override fun archiveSession(address: ZonaRosaProtocolAddress?) {
    error("Should not happen during the intended usage pattern of this class")
  }

  override fun getAllAddressesWithActiveSessions(addressNames: MutableList<String>): Map<ZonaRosaProtocolAddress, SessionRecord> {
    error("Should not happen during the intended usage pattern of this class")
  }

  fun flushToDisk(persistentStore: ZonaRosaServiceAccountDataStore) {
    for ((address, record) in updatedSessions) {
      persistentStore.storeSession(address, record)
    }

    for (address in deletedSessions) {
      persistentStore.deleteSession(address)
    }

    updatedSessions.clear()
    deletedSessions.clear()
  }
}
