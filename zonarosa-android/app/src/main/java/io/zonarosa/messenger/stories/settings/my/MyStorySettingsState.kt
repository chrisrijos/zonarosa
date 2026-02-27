package io.zonarosa.messenger.stories.settings.my

data class MyStorySettingsState(
  val myStoryPrivacyState: MyStoryPrivacyState = MyStoryPrivacyState(),
  val areRepliesAndReactionsEnabled: Boolean = false,
  val allZonaRosaConnectionsCount: Int = 0,
  val hasUserPerformedManualSelection: Boolean
)
