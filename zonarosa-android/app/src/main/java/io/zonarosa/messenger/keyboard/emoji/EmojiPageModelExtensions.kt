package io.zonarosa.messenger.keyboard.emoji

import io.zonarosa.messenger.components.emoji.EmojiPageModel
import io.zonarosa.messenger.components.emoji.EmojiPageViewGridAdapter
import io.zonarosa.messenger.components.emoji.RecentEmojiPageModel
import io.zonarosa.messenger.components.emoji.parsing.EmojiTree
import io.zonarosa.messenger.emoji.EmojiCategory
import io.zonarosa.messenger.emoji.EmojiSource
import io.zonarosa.messenger.util.adapter.mapping.MappingModel

fun EmojiPageModel.toMappingModels(): List<MappingModel<*>> {
  val emojiTree: EmojiTree = EmojiSource.latest.emojiTree

  return displayEmoji.map {
    val isTextEmoji = EmojiCategory.EMOTICONS.key == key || (RecentEmojiPageModel.KEY == key && emojiTree.getEmoji(it.value, 0, it.value.length) == null)

    if (isTextEmoji) {
      EmojiPageViewGridAdapter.EmojiTextModel(key, it)
    } else {
      EmojiPageViewGridAdapter.EmojiModel(key, it)
    }
  }
}
