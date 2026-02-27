package io.zonarosa.camera.demo

import io.zonarosa.camera.demo.screens.gallery.MediaItem

/**
 * Simple singleton to hold the currently selected media item for viewing.
 * This is used to pass data between Gallery screen and Viewer screens.
 */
object MediaSelectionHolder {
  var selectedMedia: MediaItem? = null
}
