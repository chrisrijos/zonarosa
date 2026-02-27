package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.StorageRecord

/**
 * A wrapper around [StorageRecord] to pair it with a [StorageId].
 */
data class ZonaRosaStorageRecord(
  val id: StorageId,
  val proto: StorageRecord
) {
  val isUnknown: Boolean
    get() = proto.contact == null && proto.groupV1 == null && proto.groupV2 == null && proto.account == null && proto.storyDistributionList == null && proto.callLink == null && proto.chatFolder == null && proto.notificationProfile == null

  companion object {
    @JvmStatic
    fun forUnknown(key: StorageId): ZonaRosaStorageRecord {
      return ZonaRosaStorageRecord(key, proto = StorageRecord())
    }
  }
}
