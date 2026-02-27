package io.zonarosa.messenger.conversation.colors.ui

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.conversation.colors.ChatColorsPalette
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.wallpaper.ChatWallpaper

sealed class ChatColorSelectionRepository(context: Context) {

  protected val context: Context = context.applicationContext

  abstract fun getWallpaper(consumer: (ChatWallpaper?) -> Unit)
  abstract fun getChatColors(consumer: (ChatColors) -> Unit)
  abstract fun save(chatColors: ChatColors, onSaved: () -> Unit)

  fun duplicate(chatColors: ChatColors) {
    ZonaRosaExecutors.BOUNDED.execute {
      val duplicate = chatColors.withId(ChatColors.Id.NotSet)
      ZonaRosaDatabase.chatColors.saveChatColors(duplicate)
    }
  }

  fun getUsageCount(chatColorsId: ChatColors.Id, consumer: (Int) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      consumer(ZonaRosaDatabase.recipients.getColorUsageCount(chatColorsId))
    }
  }

  fun delete(chatColors: ChatColors, onDeleted: () -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.chatColors.deleteChatColors(chatColors)
      onDeleted()
    }
  }

  private class Global(context: Context) : ChatColorSelectionRepository(context) {
    override fun getWallpaper(consumer: (ChatWallpaper?) -> Unit) {
      consumer(ZonaRosaStore.wallpaper.wallpaper)
    }

    override fun getChatColors(consumer: (ChatColors) -> Unit) {
      if (ZonaRosaStore.chatColors.hasChatColors) {
        consumer(requireNotNull(ZonaRosaStore.chatColors.chatColors))
      } else {
        getWallpaper { wallpaper ->
          if (wallpaper != null) {
            consumer(wallpaper.autoChatColors)
          } else {
            consumer(ChatColorsPalette.Bubbles.default.withId(ChatColors.Id.Auto))
          }
        }
      }
    }

    override fun save(chatColors: ChatColors, onSaved: () -> Unit) {
      if (chatColors.id == ChatColors.Id.Auto) {
        ZonaRosaStore.chatColors.chatColors = null
      } else {
        ZonaRosaStore.chatColors.chatColors = chatColors
      }
      onSaved()
    }
  }

  private class Single(context: Context, private val recipientId: RecipientId) : ChatColorSelectionRepository(context) {
    override fun getWallpaper(consumer: (ChatWallpaper?) -> Unit) {
      ZonaRosaExecutors.BOUNDED.execute {
        val recipient = Recipient.resolved(recipientId)
        consumer(recipient.wallpaper)
      }
    }

    override fun getChatColors(consumer: (ChatColors) -> Unit) {
      ZonaRosaExecutors.BOUNDED.execute {
        val recipient = Recipient.resolved(recipientId)
        consumer(recipient.chatColors)
      }
    }

    override fun save(chatColors: ChatColors, onSaved: () -> Unit) {
      ZonaRosaExecutors.BOUNDED.execute {
        val recipientDatabase = ZonaRosaDatabase.recipients
        recipientDatabase.setColor(recipientId, chatColors)
        onSaved()
      }
    }
  }

  companion object {
    fun create(context: Context, recipientId: RecipientId?): ChatColorSelectionRepository {
      return if (recipientId != null) {
        Single(context, recipientId)
      } else {
        Global(context)
      }
    }
  }
}
