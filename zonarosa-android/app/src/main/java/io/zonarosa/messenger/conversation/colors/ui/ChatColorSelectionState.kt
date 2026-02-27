package io.zonarosa.messenger.conversation.colors.ui

import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.conversation.colors.ChatColorsPalette
import io.zonarosa.messenger.util.adapter.mapping.MappingModelList
import io.zonarosa.messenger.wallpaper.ChatWallpaper

data class ChatColorSelectionState(
  val wallpaper: ChatWallpaper? = null,
  val chatColors: ChatColors? = null,
  private val chatColorOptions: List<ChatColors> = listOf()
) {

  val chatColorModels: MappingModelList

  init {
    val models: List<ChatColorMappingModel> = chatColorOptions.map { chatColors ->
      ChatColorMappingModel(
        chatColors,
        chatColors == this.chatColors,
        false
      )
    }.toList()

    val defaultModel: ChatColorMappingModel = if (wallpaper != null) {
      ChatColorMappingModel(
        wallpaper.autoChatColors,
        chatColors?.id == ChatColors.Id.Auto,
        true
      )
    } else {
      ChatColorMappingModel(
        ChatColorsPalette.Bubbles.default.withId(ChatColors.Id.Auto),
        chatColors?.id == ChatColors.Id.Auto,
        true
      )
    }

    chatColorModels = MappingModelList().apply {
      add(defaultModel)
      addAll(models)
      add(CustomColorMappingModel())
    }
  }
}
