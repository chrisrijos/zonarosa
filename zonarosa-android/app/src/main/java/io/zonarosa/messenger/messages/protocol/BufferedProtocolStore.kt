package io.zonarosa.messenger.messages.protocol

import io.zonarosa.core.models.ServiceId
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * The entry point for creating and retrieving buffered protocol stores.
 * These stores will read from disk, but never write, instead buffering the results in memory.
 * You can then call [flushToDisk] in order to write the buffered results to disk.
 *
 * This allows you to efficiently do batches of work and avoid unnecessary intermediate writes.
 */
class BufferedProtocolStore private constructor(
  private val aciStore: Pair<ServiceId, BufferedZonaRosaServiceAccountDataStore>,
  private val pniStore: Pair<ServiceId, BufferedZonaRosaServiceAccountDataStore>
) {

  fun get(serviceId: ServiceId): BufferedZonaRosaServiceAccountDataStore {
    return when (serviceId) {
      aciStore.first -> aciStore.second
      pniStore.first -> pniStore.second
      else -> error("No store matching serviceId $serviceId")
    }
  }

  fun getAciStore(): BufferedZonaRosaServiceAccountDataStore {
    return aciStore.second
  }

  /**
   * Writes any buffered data to disk. You can continue to use the same buffered store afterwards.
   */
  fun flushToDisk() {
    aciStore.second.flushToDisk(AppDependencies.protocolStore.aci())
    pniStore.second.flushToDisk(AppDependencies.protocolStore.pni())
  }

  companion object {
    fun create(): BufferedProtocolStore {
      val aci = ZonaRosaStore.account.requireAci()
      val pni = ZonaRosaStore.account.requirePni()

      return BufferedProtocolStore(
        aciStore = aci to BufferedZonaRosaServiceAccountDataStore(aci),
        pniStore = pni to BufferedZonaRosaServiceAccountDataStore(pni)
      )
    }
  }
}
