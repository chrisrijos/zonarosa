package io.zonarosa.messenger.notifications.profiles

import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.api.storage.StorageId

data class NotificationProfile(
  val id: Long,
  val name: String,
  val emoji: String,
  val color: AvatarColor = DEFAULT_NOTIFICATION_PROFILE_COLOR,
  val createdAt: Long,
  val allowAllCalls: Boolean = true,
  val allowAllMentions: Boolean = false,
  val schedule: NotificationProfileSchedule,
  val allowedMembers: Set<RecipientId> = emptySet(),
  val notificationProfileId: NotificationProfileId,
  val deletedTimestampMs: Long = 0,
  val storageServiceId: StorageId? = null,
  val storageServiceProto: ByteArray? = null
) : Comparable<NotificationProfile> {

  companion object {
    val DEFAULT_NOTIFICATION_PROFILE_COLOR = AvatarColor.A210
  }

  fun isRecipientAllowed(id: RecipientId): Boolean {
    return allowedMembers.contains(id)
  }

  override fun compareTo(other: NotificationProfile): Int {
    return createdAt.compareTo(other.createdAt)
  }
}
