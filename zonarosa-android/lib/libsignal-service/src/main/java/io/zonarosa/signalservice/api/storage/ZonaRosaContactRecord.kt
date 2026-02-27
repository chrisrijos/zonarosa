package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.ContactRecord
import java.io.IOException

/**
 * Wrapper around a [ContactRecord] to pair it with a [StorageId].
 */
data class ZonaRosaContactRecord(
  override val id: StorageId,
  override val proto: ContactRecord
) : ZonaRosaRecord<ContactRecord> {

  companion object {
    fun newBuilder(serializedUnknowns: ByteArray?): ContactRecord.Builder {
      return serializedUnknowns?.let { builderFromUnknowns(it) } ?: ContactRecord.Builder()
    }

    private fun builderFromUnknowns(serializedUnknowns: ByteArray): ContactRecord.Builder {
      return try {
        ContactRecord.ADAPTER.decode(serializedUnknowns).newBuilder()
      } catch (e: IOException) {
        ContactRecord.Builder()
      }
    }
  }
}
