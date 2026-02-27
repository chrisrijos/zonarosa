package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.NotificationProfile
import java.io.IOException

/**
 * Wrapper around a [NotificationProfile] to pair it with a [StorageId].
 */
data class ZonaRosaNotificationProfileRecord(
  override val id: StorageId,
  override val proto: NotificationProfile
) : ZonaRosaRecord<NotificationProfile> {

  companion object {
    fun newBuilder(serializedUnknowns: ByteArray?): NotificationProfile.Builder {
      return serializedUnknowns?.let { builderFromUnknowns(it) } ?: NotificationProfile.Builder()
    }

    private fun builderFromUnknowns(serializedUnknowns: ByteArray): NotificationProfile.Builder {
      return try {
        NotificationProfile.ADAPTER.decode(serializedUnknowns).newBuilder()
      } catch (e: IOException) {
        NotificationProfile.Builder()
      }
    }
  }
}
