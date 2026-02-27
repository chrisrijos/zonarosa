package io.zonarosa.messenger.conversation.colors.ui

import io.zonarosa.messenger.util.adapter.mapping.MappingModel

class CustomColorMappingModel : MappingModel<CustomColorMappingModel> {
  override fun areItemsTheSame(newItem: CustomColorMappingModel): Boolean {
    return true
  }

  override fun areContentsTheSame(newItem: CustomColorMappingModel): Boolean {
    return true
  }
}
