package io.zonarosa.messenger.avatar.text

import io.zonarosa.messenger.avatar.Avatar
import io.zonarosa.messenger.avatar.AvatarColorItem
import io.zonarosa.messenger.avatar.Avatars

data class TextAvatarCreationState(
  val currentAvatar: Avatar.Text
) {
  fun colors(): List<AvatarColorItem> = Avatars.colors.map { AvatarColorItem(it, currentAvatar.color == it) }
}
