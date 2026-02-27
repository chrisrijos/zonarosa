package io.zonarosa.messenger.components.settings.app.notifications.profiles

import io.zonarosa.messenger.notifications.profiles.NotificationProfile
import io.zonarosa.messenger.notifications.profiles.NotificationProfiles

data class NotificationProfilesState(
  val profiles: List<NotificationProfile>,
  val activeProfile: NotificationProfile? = NotificationProfiles.getActiveProfile(profiles)
)
