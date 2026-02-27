package io.zonarosa.imageeditor.app.renderers;

import io.zonarosa.imageeditor.core.Bounds;
import io.zonarosa.imageeditor.core.Renderer;

public abstract class StandardHitTestRenderer implements Renderer {

  @Override
  public boolean hitTest(float x, float y) {
    return Bounds.contains(x, y);
  }
}
