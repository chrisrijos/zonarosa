package io.zonarosa.messenger.stories.settings.privacy

import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.stories.settings.my.MyStoryPrivacyState

data class ChooseInitialMyStoryMembershipState(
  val recipientId: RecipientId? = null,
  val privacyState: MyStoryPrivacyState = MyStoryPrivacyState(),
  val allZonaRosaConnectionsCount: Int = 0,
  val hasUserPerformedManualSelection: Boolean = false
)
