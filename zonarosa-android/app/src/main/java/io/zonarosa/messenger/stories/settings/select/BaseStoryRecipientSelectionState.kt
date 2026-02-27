package io.zonarosa.messenger.stories.settings.select

import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.database.model.DistributionListRecord
import io.zonarosa.messenger.recipients.RecipientId

data class BaseStoryRecipientSelectionState(
  val distributionListId: DistributionListId?,
  val privateStory: DistributionListRecord? = null,
  val selection: Set<RecipientId> = emptySet(),
  val isStartingSelection: Boolean = false
)
