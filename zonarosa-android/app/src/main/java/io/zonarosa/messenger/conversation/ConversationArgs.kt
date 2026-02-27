/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.conversation

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import io.zonarosa.core.models.media.Media
import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.conversation.ConversationIntents.ConversationScreenType
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.mms.SlideFactory
import io.zonarosa.messenger.recipients.Recipient.Companion.resolved
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.serialization.UriSerializer
import io.zonarosa.messenger.stickers.StickerLocator
import io.zonarosa.messenger.wallpaper.ChatWallpaper

@Serializable
@Parcelize
data class ConversationArgs(
  val recipientId: RecipientId,
  @JvmField val threadId: Long,
  val draftText: String?,
  @Serializable(with = UriSerializer::class) val draftMedia: Uri?,
  val draftContentType: String?,
  val media: List<Media?>?,
  val stickerLocator: StickerLocator?,
  val isBorderless: Boolean,
  val distributionType: Int,
  val startingPosition: Int,
  val isFirstTimeInSelfCreatedGroup: Boolean,
  val isWithSearchOpen: Boolean,
  val giftBadge: Badge?,
  val shareDataTimestamp: Long,
  val conversationScreenType: ConversationScreenType
) : Parcelable {
  @IgnoredOnParcel
  val draftMediaType: SlideFactory.MediaType? = SlideFactory.MediaType.from(draftContentType)

  @IgnoredOnParcel
  val wallpaper: ChatWallpaper?
    get() = resolved(recipientId).wallpaper

  @IgnoredOnParcel
  val chatColors: ChatColors
    get() = resolved(recipientId).chatColors

  fun canInitializeFromDatabase(): Boolean {
    return draftText == null && (draftMedia == null || ConversationIntents.isBubbleIntentUri(draftMedia) || ConversationIntents.isNotificationIntentUri(draftMedia)) && draftMediaType == null
  }
}
