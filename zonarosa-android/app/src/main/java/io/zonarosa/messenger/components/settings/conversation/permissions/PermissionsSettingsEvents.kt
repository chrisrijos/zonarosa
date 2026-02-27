package io.zonarosa.messenger.components.settings.conversation.permissions

import io.zonarosa.messenger.groups.ui.GroupChangeFailureReason

sealed class PermissionsSettingsEvents {
  class GroupChangeError(val reason: GroupChangeFailureReason) : PermissionsSettingsEvents()
}
