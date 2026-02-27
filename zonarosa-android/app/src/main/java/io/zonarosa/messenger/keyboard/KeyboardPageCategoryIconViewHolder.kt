package io.zonarosa.messenger.keyboard

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.adapter.mapping.MappingModel
import io.zonarosa.messenger.util.adapter.mapping.MappingViewHolder
import java.util.function.Consumer

interface KeyboardPageCategoryIconMappingModel<T : KeyboardPageCategoryIconMappingModel<T>> : MappingModel<T> {
  val key: String
  val selected: Boolean

  fun getIcon(context: Context): Drawable
  fun getContentDescription(context: Context): String
}

class KeyboardPageCategoryIconViewHolder<T : KeyboardPageCategoryIconMappingModel<T>>(itemView: View, private val onPageSelected: Consumer<String>) : MappingViewHolder<T>(itemView) {

  private val iconView: AppCompatImageView = itemView.findViewById(R.id.category_icon)
  private val iconSelected: View = itemView.findViewById(R.id.category_icon_selected)

  override fun bind(model: T) {
    itemView.setOnClickListener {
      onPageSelected.accept(model.key)
    }

    iconView.setImageDrawable(model.getIcon(context))
    iconView.contentDescription = model.getContentDescription(context)
    iconView.isSelected = model.selected
    iconSelected.isSelected = model.selected
  }
}
