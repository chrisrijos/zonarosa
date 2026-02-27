package io.zonarosa.messenger.scribbles;

import androidx.annotation.NonNull;

import io.zonarosa.imageeditor.core.HiddenEditText;
import io.zonarosa.messenger.components.emoji.EmojiUtil;

class RemoveEmojiTextFilter implements HiddenEditText.TextFilter {
  @Override
  public String filter(@NonNull String text) {
    return EmojiUtil.stripEmoji(text);
  }
}
