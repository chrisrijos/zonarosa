/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.exporters

import android.database.Cursor
import io.zonarosa.core.util.decodeOrNull
import io.zonarosa.core.util.requireBlob
import io.zonarosa.core.util.requireBoolean
import io.zonarosa.core.util.requireInt
import io.zonarosa.core.util.requireIntOrNull
import io.zonarosa.core.util.requireLong
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.proto.Chat
import io.zonarosa.messenger.backup.v2.util.ChatStyleConverter
import io.zonarosa.messenger.backup.v2.util.isValid
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.ThreadTable
import io.zonarosa.messenger.database.model.databaseprotos.ChatColor
import io.zonarosa.messenger.database.model.databaseprotos.Wallpaper
import java.io.Closeable
import kotlin.time.Duration.Companion.seconds

class ChatArchiveExporter(private val cursor: Cursor, private val db: ZonaRosaDatabase, private val exportState: ExportState, private val includeImageWallpapers: Boolean) : Iterator<Chat>, Closeable {
  override fun hasNext(): Boolean {
    return cursor.count > 0 && !cursor.isLast
  }

  override fun next(): Chat {
    if (!cursor.moveToNext()) {
      throw NoSuchElementException()
    }

    val customChatColorsId = ChatColors.Id.forLongValue(cursor.requireLong(RecipientTable.CUSTOM_CHAT_COLORS_ID)).takeIf { it.isValid(exportState) } ?: ChatColors.Id.NotSet

    val chatColors: ChatColors? = cursor.requireBlob(RecipientTable.CHAT_COLORS)?.takeUnless { customChatColorsId is ChatColors.Id.NotSet }?.let { serializedChatColors ->
      val chatColor = ChatColor.ADAPTER.decodeOrNull(serializedChatColors)
      chatColor?.let { ChatColors.forChatColor(customChatColorsId, it) }
    }

    val chatWallpaper: Wallpaper? = cursor.requireBlob(RecipientTable.WALLPAPER)?.let { serializedWallpaper ->
      val wallpaper = Wallpaper.ADAPTER.decodeOrNull(serializedWallpaper)
      val isImageWallpaper = wallpaper?.file_ != null

      if (includeImageWallpapers || !isImageWallpaper) {
        wallpaper
      } else {
        null
      }
    }

    return Chat(
      id = cursor.requireLong(ThreadTable.ID),
      recipientId = cursor.requireLong(ThreadTable.RECIPIENT_ID),
      archived = cursor.requireBoolean(ThreadTable.ARCHIVED),
      pinnedOrder = cursor.requireIntOrNull(ThreadTable.PINNED_ORDER),
      expirationTimerMs = cursor.requireLong(RecipientTable.MESSAGE_EXPIRATION_TIME).seconds.inWholeMilliseconds.takeIf { it > 0 },
      expireTimerVersion = cursor.requireInt(RecipientTable.MESSAGE_EXPIRATION_TIME_VERSION),
      muteUntilMs = cursor.requireLong(RecipientTable.MUTE_UNTIL).takeIf { it > 0 },
      markedUnread = ThreadTable.ReadStatus.deserialize(cursor.requireInt(ThreadTable.READ)) == ThreadTable.ReadStatus.FORCED_UNREAD,
      dontNotifyForMentionsIfMuted = RecipientTable.MentionSetting.DO_NOT_NOTIFY.id == cursor.requireInt(RecipientTable.MENTION_SETTING),
      style = ChatStyleConverter.constructRemoteChatStyle(
        db = db,
        chatColors = chatColors,
        chatColorId = customChatColorsId,
        chatWallpaper = chatWallpaper,
        backupMode = exportState.backupMode
      )
    )
  }

  override fun close() {
    cursor.close()
  }
}
