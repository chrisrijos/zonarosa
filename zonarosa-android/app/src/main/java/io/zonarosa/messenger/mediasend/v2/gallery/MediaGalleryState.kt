package io.zonarosa.messenger.mediasend.v2.gallery

import io.zonarosa.messenger.util.adapter.mapping.MappingModel

data class MediaGalleryState(
  val bucketId: String?,
  val bucketTitle: String?,
  val items: List<MappingModel<*>> = listOf()
)
