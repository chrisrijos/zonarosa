package io.zonarosa.messenger.keyboard.emoji

import android.content.Context
import android.graphics.drawable.Drawable
import io.zonarosa.core.ui.util.ThemeUtil
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.emoji.RecentEmojiPageModel
import io.zonarosa.messenger.emoji.EmojiCategory
import io.zonarosa.messenger.keyboard.KeyboardPageCategoryIconMappingModel

class RecentsMappingModel(override val selected: Boolean) : KeyboardPageCategoryIconMappingModel<RecentsMappingModel> {
  override val key: String = RecentEmojiPageModel.KEY

  override fun getIcon(context: Context): Drawable {
    return requireNotNull(ThemeUtil.getThemedDrawable(context, R.attr.emoji_category_recent))
  }

  override fun getContentDescription(context: Context): String {
    return context.getString(R.string.ReactWithAnyEmojiBottomSheetDialogFragment__recents)
  }

  override fun areItemsTheSame(newItem: RecentsMappingModel): Boolean {
    return newItem.key == key
  }

  override fun areContentsTheSame(newItem: RecentsMappingModel): Boolean {
    return areItemsTheSame(newItem) && selected == newItem.selected
  }
}

class EmojiCategoryMappingModel(private val emojiCategory: EmojiCategory, override val selected: Boolean) : KeyboardPageCategoryIconMappingModel<EmojiCategoryMappingModel> {
  override val key: String = emojiCategory.key

  override fun getIcon(context: Context): Drawable {
    return requireNotNull(ThemeUtil.getThemedDrawable(context, emojiCategory.icon))
  }

  override fun getContentDescription(context: Context): String {
    return context.getString(emojiCategory.getCategoryLabel())
  }

  override fun areItemsTheSame(newItem: EmojiCategoryMappingModel): Boolean {
    return newItem.key == key
  }

  override fun areContentsTheSame(newItem: EmojiCategoryMappingModel): Boolean {
    return areItemsTheSame(newItem) &&
      selected == newItem.selected &&
      newItem.emojiCategory == emojiCategory
  }
}
