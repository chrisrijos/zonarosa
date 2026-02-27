package io.zonarosa.messenger.conversation.ui.inlinequery

import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.adapter.mapping.AnyMappingModel
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter

class InlineQueryAdapter(listener: (AnyMappingModel) -> Unit) : MappingAdapter() {
  init {
    registerFactory(InlineQueryEmojiResult.Model::class.java, { InlineQueryEmojiResult.ViewHolder(it, listener) }, R.layout.inline_query_emoji_result)
  }
}
