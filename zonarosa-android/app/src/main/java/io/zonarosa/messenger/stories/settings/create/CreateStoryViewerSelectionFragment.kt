package io.zonarosa.messenger.stories.settings.create

import androidx.navigation.fragment.findNavController
import io.zonarosa.messenger.R
import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.stories.settings.select.BaseStoryRecipientSelectionFragment
import io.zonarosa.messenger.util.navigation.safeNavigate

/**
 * Allows user to select who will see the story they are creating
 */
class CreateStoryViewerSelectionFragment : BaseStoryRecipientSelectionFragment() {
  override val actionButtonLabel: Int = R.string.CreateStoryViewerSelectionFragment__next
  override val distributionListId: DistributionListId? = null

  override fun goToNextScreen(recipients: Set<RecipientId>) {
    findNavController().safeNavigate(CreateStoryViewerSelectionFragmentDirections.actionCreateStoryViewerSelectionToCreateStoryWithViewers(recipients.toTypedArray()))
  }
}
