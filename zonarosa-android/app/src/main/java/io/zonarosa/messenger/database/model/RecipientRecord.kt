package io.zonarosa.messenger.database.model

import android.net.Uri
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredential
import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.database.IdentityTable.VerifiedStatus
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.RecipientTable.MentionSetting
import io.zonarosa.messenger.database.RecipientTable.PhoneNumberSharingState
import io.zonarosa.messenger.database.RecipientTable.RegisteredState
import io.zonarosa.messenger.database.RecipientTable.SealedSenderAccessMode
import io.zonarosa.messenger.database.RecipientTable.VibrateState
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.wallpaper.ChatWallpaper

/**
 * Database model for [RecipientTable].
 */
data class RecipientRecord(
  val id: RecipientId,
  val aci: ACI?,
  val pni: PNI?,
  val username: String?,
  val e164: String?,
  val email: String?,
  val groupId: GroupId?,
  val distributionListId: DistributionListId?,
  val recipientType: RecipientTable.RecipientType,
  val isBlocked: Boolean,
  val muteUntil: Long,
  val messageVibrateState: VibrateState,
  val callVibrateState: VibrateState,
  val messageRingtone: Uri?,
  val callRingtone: Uri?,
  val expireMessages: Int,
  val expireTimerVersion: Int,
  val registered: RegisteredState,
  val profileKey: ByteArray?,
  val expiringProfileKeyCredential: ExpiringProfileKeyCredential?,
  val systemProfileName: ProfileName,
  val systemDisplayName: String?,
  val systemContactPhotoUri: String?,
  val systemPhoneLabel: String?,
  val systemContactUri: String?,
  @get:JvmName("getProfileName")
  val zonarosaProfileName: ProfileName,
  @get:JvmName("getProfileAvatar")
  val zonarosaProfileAvatar: String?,
  val profileAvatarFileDetails: ProfileAvatarFileDetails,
  @get:JvmName("isProfileSharing")
  val profileSharing: Boolean,
  val lastProfileFetch: Long,
  val notificationChannel: String?,
  val sealedSenderAccessMode: SealedSenderAccessMode,
  val capabilities: Capabilities,
  val storageId: ByteArray?,
  val mentionSetting: MentionSetting,
  val wallpaper: ChatWallpaper?,
  val chatColors: ChatColors?,
  val avatarColor: AvatarColor,
  val about: String?,
  val aboutEmoji: String?,
  val syncExtras: SyncExtras,
  val extras: Recipient.Extras?,
  @get:JvmName("hasGroupsInCommon")
  val hasGroupsInCommon: Boolean,
  val badges: List<Badge>,
  @get:JvmName("needsPniSignature")
  val needsPniSignature: Boolean,
  val hiddenState: Recipient.HiddenState,
  val callLinkRoomId: CallLinkRoomId?,
  val phoneNumberSharing: PhoneNumberSharingState,
  val nickname: ProfileName,
  val note: String?,
  val keyTransparencyData: ByteArray? = null
) {

  fun e164Only(): Boolean {
    return this.e164 != null && this.aci == null && this.pni == null
  }

  fun pniOnly(): Boolean {
    return this.e164 == null && this.aci == null && this.pni != null
  }

  fun aciOnly(): Boolean {
    return this.e164 == null && this.pni == null && this.aci != null
  }

  fun pniAndAci(): Boolean {
    return this.aci != null && this.pni != null
  }

  val serviceId: ServiceId? = this.aci ?: this.pni

  /**
   * A bundle of data that's only necessary when syncing to storage service, not for a
   * [Recipient].
   */
  data class SyncExtras(
    val storageProto: ByteArray?,
    val groupMasterKey: GroupMasterKey?,
    val identityKey: ByteArray?,
    val identityStatus: VerifiedStatus,
    val isArchived: Boolean,
    val isForcedUnread: Boolean,
    val unregisteredTimestamp: Long,
    val systemNickname: String?,
    val pniSignatureVerified: Boolean
  )

  data class Capabilities(
    val rawBits: Long
  ) {
    companion object {
      @JvmField
      val UNKNOWN = Capabilities(
        rawBits = 0
      )
    }
  }
}
