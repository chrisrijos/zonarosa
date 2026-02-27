package io.zonarosa.messenger.keyboard.gif

import android.view.View
import android.widget.ImageView
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.adapter.mapping.LayoutFactory
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import io.zonarosa.messenger.util.adapter.mapping.MappingViewHolder

class GifQuickSearchAdapter(clickListener: (GifQuickSearchOption) -> Unit) : MappingAdapter() {
  init {
    registerFactory(GifQuickSearch::class.java, LayoutFactory({ v -> ViewHolder(v, clickListener) }, R.layout.keyboard_pager_category_icon))
  }

  private class ViewHolder(itemView: View, private val listener: (GifQuickSearchOption) -> Unit) : MappingViewHolder<GifQuickSearch>(itemView) {
    private val image: ImageView = findViewById(R.id.category_icon)
    private val imageSelected: View = findViewById(R.id.category_icon_selected)

    override fun bind(model: GifQuickSearch) {
      image.setImageResource(model.gifQuickSearchOption.image)
      image.isSelected = model.selected
      image.contentDescription = itemView.context.getString(model.gifQuickSearchOption.categoryLabel)
      imageSelected.isSelected = model.selected
      itemView.setOnClickListener { listener(model.gifQuickSearchOption) }
    }
  }
}
