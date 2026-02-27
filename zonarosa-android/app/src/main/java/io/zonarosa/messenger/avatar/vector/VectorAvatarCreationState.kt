package io.zonarosa.messenger.avatar.vector

import io.zonarosa.messenger.avatar.Avatar
import io.zonarosa.messenger.avatar.AvatarColorItem
import io.zonarosa.messenger.avatar.Avatars

data class VectorAvatarCreationState(
  val currentAvatar: Avatar.Vector
) {
  fun colors(): List<AvatarColorItem> = Avatars.colors.map { AvatarColorItem(it, currentAvatar.color == it) }
}
