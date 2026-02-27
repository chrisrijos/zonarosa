/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import android.content.Context
import android.database.Cursor
import com.google.protobuf.InvalidProtocolBufferException
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.optionalBlob
import io.zonarosa.core.util.optionalBoolean
import io.zonarosa.core.util.optionalInt
import io.zonarosa.core.util.optionalLong
import io.zonarosa.core.util.optionalString
import io.zonarosa.core.util.requireBlob
import io.zonarosa.core.util.requireBoolean
import io.zonarosa.core.util.requireInt
import io.zonarosa.core.util.requireLong
import io.zonarosa.core.util.requireString
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredential
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.crypto.ProfileKeyUtil
import io.zonarosa.messenger.database.IdentityTable.VerifiedStatus
import io.zonarosa.messenger.database.RecipientTable.RegisteredState
import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.database.model.RecipientRecord
import io.zonarosa.messenger.database.model.databaseprotos.BadgeList
import io.zonarosa.messenger.database.model.databaseprotos.ChatColor
import io.zonarosa.messenger.database.model.databaseprotos.ExpiringProfileKeyCredentialColumnData
import io.zonarosa.messenger.database.model.databaseprotos.RecipientExtras
import io.zonarosa.messenger.database.model.databaseprotos.Wallpaper
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.profiles.AvatarHelper
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.util.GroupUtil
import io.zonarosa.messenger.wallpaper.ChatWallpaper
import io.zonarosa.messenger.wallpaper.ChatWallpaperFactory
import java.io.IOException

object RecipientTableCursorUtil {

  private val TAG = Log.tag(RecipientTableCursorUtil::class.java)

  fun getRecord(context: Context, cursor: Cursor): RecipientRecord {
    return getRecord(context, cursor, RecipientTable.ID)
  }

  fun getRecord(context: Context, cursor: Cursor, idColumnName: String): RecipientRecord {
    val profileKeyString = cursor.requireString(RecipientTable.PROFILE_KEY)
    val expiringProfileKeyCredentialString = cursor.requireString(RecipientTable.EXPIRING_PROFILE_KEY_CREDENTIAL)
    var profileKey: ProfileKey? = null
    var expiringProfileKeyCredential: ExpiringProfileKeyCredential? = null

    if (profileKeyString != null) {
      profileKey = ProfileKeyUtil.profileKeyOrNull(profileKeyString)

      if (profileKey == null) {
        Log.w(TAG, "Profile key present in db but is invalid, ignoring on read")
      } else if (expiringProfileKeyCredentialString != null) {
        try {
          val columnDataBytes = Base64.decode(expiringProfileKeyCredentialString)
          val columnData = ExpiringProfileKeyCredentialColumnData.ADAPTER.decode(columnDataBytes)
          if (columnData.profileKey.toByteArray().contentEquals(profileKey.serialize())) {
            expiringProfileKeyCredential = ExpiringProfileKeyCredential(columnData.expiringProfileKeyCredential.toByteArray())
          } else {
            Log.i(TAG, "Out of date profile key credential data ignored on read")
          }
        } catch (e: InvalidInputException) {
          Log.w(TAG, "Profile key credential column data could not be read", e)
        } catch (e: IOException) {
          Log.w(TAG, "Profile key credential column data could not be read", e)
        }
      }
    }

    val serializedWallpaper = cursor.requireBlob(RecipientTable.WALLPAPER)
    val chatWallpaper: ChatWallpaper? = if (serializedWallpaper != null) {
      try {
        ChatWallpaperFactory.create(Wallpaper.ADAPTER.decode(serializedWallpaper))
      } catch (e: InvalidProtocolBufferException) {
        Log.w(TAG, "Failed to parse wallpaper.", e)
        null
      }
    } else {
      null
    }

    val customChatColorsId = cursor.requireLong(RecipientTable.CUSTOM_CHAT_COLORS_ID)
    val serializedChatColors = cursor.requireBlob(RecipientTable.CHAT_COLORS)
    val chatColors: ChatColors? = if (serializedChatColors != null) {
      try {
        ChatColors.forChatColor(ChatColors.Id.forLongValue(customChatColorsId), ChatColor.ADAPTER.decode(serializedChatColors))
      } catch (e: InvalidProtocolBufferException) {
        Log.w(TAG, "Failed to parse chat colors.", e)
        null
      }
    } else {
      null
    }

    val recipientId = RecipientId.from(cursor.requireLong(idColumnName))
    val distributionListId: DistributionListId? = DistributionListId.fromNullable(cursor.requireLong(RecipientTable.DISTRIBUTION_LIST_ID))
    val avatarColor: AvatarColor = if (distributionListId != null) AvatarColor.UNKNOWN else AvatarColor.deserialize(cursor.requireString(RecipientTable.AVATAR_COLOR))

    return RecipientRecord(
      id = recipientId,
      aci = ServiceId.ACI.parseOrNull(cursor.requireString(RecipientTable.ACI_COLUMN)),
      pni = ServiceId.PNI.parsePrefixedOrNull(cursor.requireString(RecipientTable.PNI_COLUMN)),
      username = cursor.requireString(RecipientTable.USERNAME),
      e164 = cursor.requireString(RecipientTable.E164),
      email = cursor.requireString(RecipientTable.EMAIL),
      groupId = GroupId.parseNullableOrThrow(cursor.requireString(RecipientTable.GROUP_ID)),
      distributionListId = distributionListId,
      recipientType = RecipientTable.RecipientType.fromId(cursor.requireInt(RecipientTable.TYPE)),
      isBlocked = cursor.requireBoolean(RecipientTable.BLOCKED),
      muteUntil = cursor.requireLong(RecipientTable.MUTE_UNTIL),
      messageVibrateState = RecipientTable.VibrateState.fromId(cursor.requireInt(RecipientTable.MESSAGE_VIBRATE)),
      callVibrateState = RecipientTable.VibrateState.fromId(cursor.requireInt(RecipientTable.CALL_VIBRATE)),
      messageRingtone = Util.uri(cursor.requireString(RecipientTable.MESSAGE_RINGTONE)),
      callRingtone = Util.uri(cursor.requireString(RecipientTable.CALL_RINGTONE)),
      expireMessages = cursor.requireInt(RecipientTable.MESSAGE_EXPIRATION_TIME),
      expireTimerVersion = cursor.requireInt(RecipientTable.MESSAGE_EXPIRATION_TIME_VERSION),
      registered = RegisteredState.fromId(cursor.requireInt(RecipientTable.REGISTERED)),
      profileKey = profileKey?.serialize(),
      expiringProfileKeyCredential = expiringProfileKeyCredential,
      systemProfileName = ProfileName.fromParts(cursor.requireString(RecipientTable.SYSTEM_GIVEN_NAME), cursor.requireString(RecipientTable.SYSTEM_FAMILY_NAME)),
      systemDisplayName = cursor.requireString(RecipientTable.SYSTEM_JOINED_NAME),
      systemContactPhotoUri = cursor.requireString(RecipientTable.SYSTEM_PHOTO_URI),
      systemPhoneLabel = cursor.requireString(RecipientTable.SYSTEM_PHONE_LABEL),
      systemContactUri = cursor.requireString(RecipientTable.SYSTEM_CONTACT_URI),
      zonarosaProfileName = ProfileName.fromParts(cursor.requireString(RecipientTable.PROFILE_GIVEN_NAME), cursor.requireString(RecipientTable.PROFILE_FAMILY_NAME)),
      zonarosaProfileAvatar = cursor.requireString(RecipientTable.PROFILE_AVATAR),
      profileAvatarFileDetails = AvatarHelper.getAvatarFileDetails(context, recipientId),
      profileSharing = cursor.requireBoolean(RecipientTable.PROFILE_SHARING),
      lastProfileFetch = cursor.requireLong(RecipientTable.LAST_PROFILE_FETCH),
      notificationChannel = cursor.requireString(RecipientTable.NOTIFICATION_CHANNEL),
      sealedSenderAccessMode = RecipientTable.SealedSenderAccessMode.fromMode(cursor.requireInt(RecipientTable.SEALED_SENDER_MODE)),
      capabilities = readCapabilities(cursor),
      storageId = Base64.decodeNullableOrThrow(cursor.requireString(RecipientTable.STORAGE_SERVICE_ID)),
      mentionSetting = RecipientTable.MentionSetting.fromId(cursor.requireInt(RecipientTable.MENTION_SETTING)),
      wallpaper = chatWallpaper,
      chatColors = chatColors,
      avatarColor = avatarColor,
      about = cursor.requireString(RecipientTable.ABOUT),
      aboutEmoji = cursor.requireString(RecipientTable.ABOUT_EMOJI),
      syncExtras = getSyncExtras(cursor),
      extras = getExtras(cursor),
      hasGroupsInCommon = cursor.requireBoolean(RecipientTable.GROUPS_IN_COMMON),
      badges = parseBadgeList(cursor.requireBlob(RecipientTable.BADGES)),
      needsPniSignature = cursor.requireBoolean(RecipientTable.NEEDS_PNI_SIGNATURE),
      hiddenState = Recipient.HiddenState.deserialize(cursor.requireInt(RecipientTable.HIDDEN)),
      callLinkRoomId = cursor.requireString(RecipientTable.CALL_LINK_ROOM_ID)?.let { CallLinkRoomId.DatabaseSerializer.deserialize(it) },
      phoneNumberSharing = cursor.requireInt(RecipientTable.PHONE_NUMBER_SHARING).let { RecipientTable.PhoneNumberSharingState.fromId(it) },
      nickname = ProfileName.fromParts(cursor.requireString(RecipientTable.NICKNAME_GIVEN_NAME), cursor.requireString(RecipientTable.NICKNAME_FAMILY_NAME)),
      note = cursor.requireString(RecipientTable.NOTE),
      keyTransparencyData = cursor.requireBlob(RecipientTable.KEY_TRANSPARENCY_DATA)
    )
  }

  fun readCapabilities(cursor: Cursor): RecipientRecord.Capabilities {
    val capabilities = cursor.requireLong(RecipientTable.CAPABILITIES)
    return RecipientRecord.Capabilities(
      rawBits = capabilities
    )
  }

  fun parseBadgeList(serializedBadgeList: ByteArray?): List<Badge> {
    var badgeList: BadgeList? = null
    if (serializedBadgeList != null) {
      try {
        badgeList = BadgeList.ADAPTER.decode(serializedBadgeList)
      } catch (e: InvalidProtocolBufferException) {
        Log.w(TAG, e)
      }
    }

    val badges: List<Badge>
    if (badgeList != null) {
      val protoBadges = badgeList.badges
      badges = ArrayList(protoBadges.size)
      for (protoBadge in protoBadges) {
        badges.add(Badges.fromDatabaseBadge(protoBadge))
      }
    } else {
      badges = emptyList()
    }

    return badges
  }

  fun getSyncExtras(cursor: Cursor): RecipientRecord.SyncExtras {
    return RecipientRecord.SyncExtras(
      storageProto = cursor.optionalString(RecipientTable.STORAGE_SERVICE_PROTO).orElse(null)?.let { Base64.decodeOrThrow(it) },
      groupMasterKey = cursor.optionalBlob(GroupTable.V2_MASTER_KEY).map { GroupUtil.requireMasterKey(it) }.orElse(null),
      identityKey = cursor.optionalString(RecipientTable.IDENTITY_KEY).map { Base64.decodeOrThrow(it) }.orElse(null),
      identityStatus = cursor.optionalInt(RecipientTable.IDENTITY_STATUS).map { VerifiedStatus.forState(it) }.orElse(VerifiedStatus.DEFAULT),
      isArchived = cursor.optionalBoolean(ThreadTable.ARCHIVED).orElse(false),
      isForcedUnread = cursor.optionalInt(ThreadTable.READ).map { status: Int -> status == ThreadTable.ReadStatus.FORCED_UNREAD.serialize() }.orElse(false),
      unregisteredTimestamp = cursor.optionalLong(RecipientTable.UNREGISTERED_TIMESTAMP).orElse(0),
      systemNickname = cursor.optionalString(RecipientTable.SYSTEM_NICKNAME).orElse(null),
      pniSignatureVerified = cursor.optionalBoolean(RecipientTable.PNI_SIGNATURE_VERIFIED).orElse(false)
    )
  }

  fun getExtras(cursor: Cursor): Recipient.Extras? {
    return Recipient.Extras.from(getRecipientExtras(cursor))
  }

  fun getRecipientExtras(cursor: Cursor): RecipientExtras? {
    return cursor.optionalBlob(RecipientTable.EXTRAS).map { b: ByteArray ->
      try {
        RecipientExtras.ADAPTER.decode(b)
      } catch (e: InvalidProtocolBufferException) {
        Log.w(TAG, e)
        throw AssertionError(e)
      }
    }.orElse(null)
  }
}
