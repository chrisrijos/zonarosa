package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.GroupV2Record
import java.io.IOException

/**
 * Wrapper around a [GroupV2Record] to pair it with a [StorageId].
 */
data class ZonaRosaGroupV2Record(
  override val id: StorageId,
  override val proto: GroupV2Record
) : ZonaRosaRecord<GroupV2Record> {

  companion object {
    fun newBuilder(serializedUnknowns: ByteArray?): GroupV2Record.Builder {
      return serializedUnknowns?.let { builderFromUnknowns(it) } ?: GroupV2Record.Builder()
    }

    private fun builderFromUnknowns(serializedUnknowns: ByteArray): GroupV2Record.Builder {
      return try {
        GroupV2Record.ADAPTER.decode(serializedUnknowns).newBuilder()
      } catch (e: IOException) {
        GroupV2Record.Builder()
      }
    }
  }
}
