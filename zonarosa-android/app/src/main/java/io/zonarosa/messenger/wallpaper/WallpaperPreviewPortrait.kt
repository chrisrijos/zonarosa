package io.zonarosa.messenger.wallpaper

import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.AvatarImageView
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.recipients.Recipient

sealed class WallpaperPreviewPortrait {
  class ContactPhoto(private val recipient: Recipient) : WallpaperPreviewPortrait() {
    override fun applyToAvatarImageView(avatarImageView: AvatarImageView) {
      avatarImageView.setAvatar(recipient)
      avatarImageView.colorFilter = null
    }
  }

  class SolidColor(private val avatarColor: AvatarColor) : WallpaperPreviewPortrait() {
    override fun applyToAvatarImageView(avatarImageView: AvatarImageView) {
      avatarImageView.setImageResource(R.drawable.circle_tintable)
      avatarImageView.setColorFilter(avatarColor.colorInt())
    }
  }

  abstract fun applyToAvatarImageView(avatarImageView: AvatarImageView)
}
