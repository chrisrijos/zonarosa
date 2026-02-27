package io.zonarosa.messenger.components.settings.conversation

import io.zonarosa.messenger.util.DynamicNoActionBarTheme
import io.zonarosa.messenger.util.DynamicTheme

class CallInfoActivity : ConversationSettingsActivity(), ConversationSettingsFragment.Callback {

  override val dynamicTheme: DynamicTheme = DynamicNoActionBarTheme()
}
