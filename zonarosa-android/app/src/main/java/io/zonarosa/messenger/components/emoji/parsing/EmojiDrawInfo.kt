package io.zonarosa.messenger.components.emoji.parsing

import io.zonarosa.messenger.emoji.EmojiPage

data class EmojiDrawInfo(val page: EmojiPage, val index: Int, val emoji: String, val rawEmoji: String?, val jumboSheet: String?)
