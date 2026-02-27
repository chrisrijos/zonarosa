package io.zonarosa.messenger.storage

import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.SqlUtil
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.NotificationProfileTables
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.notifications.profiles.NotificationProfileId
import io.zonarosa.service.api.storage.ZonaRosaNotificationProfileRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.util.OptionalUtil.asOptional
import io.zonarosa.service.internal.storage.protos.Recipient
import java.util.Optional
import java.util.UUID

/**
 * Record processor for [ZonaRosaNotificationProfileRecord].
 * Handles merging and updating our local store when processing remote notification profile storage records.
 */
class NotificationProfileRecordProcessor : DefaultStorageRecordProcessor<ZonaRosaNotificationProfileRecord>() {

  companion object {
    private val TAG = Log.tag(NotificationProfileRecordProcessor::class)
  }

  override fun compare(o1: ZonaRosaNotificationProfileRecord, o2: ZonaRosaNotificationProfileRecord): Int {
    return if (o1.proto.id == o2.proto.id) {
      0
    } else {
      1
    }
  }

  /**
   * Notification profiles must have a valid identifier
   * Notification profiles must have a name
   * All allowed members must have a valid serviceId
   */
  override fun isInvalid(remote: ZonaRosaNotificationProfileRecord): Boolean {
    return UuidUtil.parseOrNull(remote.proto.id) == null ||
      remote.proto.name.isEmpty() ||
      containsInvalidServiceId(remote.proto.allowedMembers)
  }

  override fun getMatching(remote: ZonaRosaNotificationProfileRecord, keyGenerator: StorageKeyGenerator): Optional<ZonaRosaNotificationProfileRecord> {
    Log.d(TAG, "Attempting to get matching record...")
    val uuid: UUID = UuidUtil.parseOrThrow(remote.proto.id)
    val query = SqlUtil.buildQuery("${NotificationProfileTables.NotificationProfileTable.NOTIFICATION_PROFILE_ID} = ?", NotificationProfileId(uuid))

    val notificationProfile = ZonaRosaDatabase.notificationProfiles.getProfile(query)

    return if (notificationProfile?.storageServiceId != null) {
      StorageSyncModels.localToRemoteNotificationProfile(notificationProfile, notificationProfile.storageServiceId.raw).asOptional()
    } else if (notificationProfile != null) {
      Log.d(TAG, "Notification profile was missing a storage service id, generating one")
      val storageId = StorageId.forNotificationProfile(keyGenerator.generate())
      ZonaRosaDatabase.notificationProfiles.applyStorageIdUpdate(notificationProfile.notificationProfileId, storageId)
      StorageSyncModels.localToRemoteNotificationProfile(notificationProfile, storageId.raw).asOptional()
    } else {
      Log.d(TAG, "Could not find a matching record. Returning an empty.")
      Optional.empty<ZonaRosaNotificationProfileRecord>()
    }
  }

  /**
   * A deleted record takes precedence over a non-deleted record
   * while an earlier deletion takes precedence over a later deletion
   */
  override fun merge(remote: ZonaRosaNotificationProfileRecord, local: ZonaRosaNotificationProfileRecord, keyGenerator: StorageKeyGenerator): ZonaRosaNotificationProfileRecord {
    val isRemoteDeleted = remote.proto.deletedAtTimestampMs > 0
    val isLocalDeleted = local.proto.deletedAtTimestampMs > 0

    return when {
      isRemoteDeleted && isLocalDeleted -> if (remote.proto.deletedAtTimestampMs <= local.proto.deletedAtTimestampMs) remote else local
      isRemoteDeleted -> remote
      isLocalDeleted -> local
      else -> remote
    }
  }

  override fun insertLocal(record: ZonaRosaNotificationProfileRecord) {
    ZonaRosaDatabase.notificationProfiles.insertNotificationProfileFromStorageSync(record)
  }

  override fun updateLocal(update: StorageRecordUpdate<ZonaRosaNotificationProfileRecord>) {
    ZonaRosaDatabase.notificationProfiles.updateNotificationProfileFromStorageSync(update.new)
  }

  private fun containsInvalidServiceId(recipients: List<Recipient>): Boolean {
    return recipients.any { recipient ->
      recipient.contact != null && ServiceId.parseOrNull(recipient.contact!!.serviceId, recipient.contact!!.serviceIdBinary) == null
    }
  }
}
