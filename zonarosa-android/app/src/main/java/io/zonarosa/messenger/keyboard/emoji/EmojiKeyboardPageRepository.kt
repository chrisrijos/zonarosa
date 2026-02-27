package io.zonarosa.messenger.keyboard.emoji

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.components.emoji.EmojiPageModel
import io.zonarosa.messenger.components.emoji.RecentEmojiPageModel
import io.zonarosa.messenger.emoji.EmojiSource.Companion.latest
import io.zonarosa.messenger.util.ZonaRosaPreferences
import java.util.function.Consumer

class EmojiKeyboardPageRepository(private val context: Context) {
  fun getEmoji(consumer: Consumer<List<EmojiPageModel>>) {
    ZonaRosaExecutors.BOUNDED.execute {
      val list = mutableListOf<EmojiPageModel>()
      list += RecentEmojiPageModel(context, ZonaRosaPreferences.RECENT_STORAGE_KEY)
      list += latest.displayPages
      consumer.accept(list)
    }
  }
}
