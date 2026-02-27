package io.zonarosa.messenger.scribbles.stickers

import io.zonarosa.imageeditor.core.Renderer

/**
 * A renderer that can handle a tap event
 */
interface TappableRenderer : Renderer {
  fun onTapped()
}
