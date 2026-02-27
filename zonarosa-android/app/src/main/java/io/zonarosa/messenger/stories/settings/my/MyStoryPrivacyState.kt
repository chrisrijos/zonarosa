package io.zonarosa.messenger.stories.settings.my

import io.zonarosa.messenger.database.model.DistributionListPrivacyMode

data class MyStoryPrivacyState(val privacyMode: DistributionListPrivacyMode? = null, val connectionCount: Int = 0)
