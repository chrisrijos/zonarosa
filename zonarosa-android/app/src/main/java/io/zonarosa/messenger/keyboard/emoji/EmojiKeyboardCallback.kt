package io.zonarosa.messenger.keyboard.emoji

import io.zonarosa.messenger.components.emoji.EmojiEventListener
import io.zonarosa.messenger.keyboard.emoji.search.EmojiSearchFragment

interface EmojiKeyboardCallback :
  EmojiEventListener,
  EmojiKeyboardPageFragment.Callback,
  EmojiSearchFragment.Callback
