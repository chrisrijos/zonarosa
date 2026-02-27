package io.zonarosa.camera.demo.screens.gallery

sealed interface GalleryScreenEvents {
  data class OnMediaItemClick(val mediaItem: MediaItem) : GalleryScreenEvents
  data object OnRefresh : GalleryScreenEvents
}
