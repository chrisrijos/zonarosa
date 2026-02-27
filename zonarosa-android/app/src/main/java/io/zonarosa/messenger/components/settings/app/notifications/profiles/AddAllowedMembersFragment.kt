package io.zonarosa.messenger.components.settings.app.notifications.profiles

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.rx3.asFlow
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.Rows
import io.zonarosa.core.ui.compose.Scaffolds
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.core.ui.compose.Snackbars
import io.zonarosa.core.ui.compose.Texts
import io.zonarosa.core.ui.compose.horizontalGutters
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.emoji.EmojiStrings
import io.zonarosa.messenger.components.settings.app.notifications.profiles.AddAllowedMembersViewModel.NotificationProfileAndRecipients
import io.zonarosa.messenger.compose.rememberStatusBarColorNestedScrollModifier
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.notifications.profiles.NotificationProfile
import io.zonarosa.messenger.notifications.profiles.NotificationProfileId
import io.zonarosa.messenger.notifications.profiles.NotificationProfileSchedule
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.navigation.safeNavigate
import java.util.UUID

/**
 * Show and allow addition of recipients to a profile during the create flow.
 */
class AddAllowedMembersFragment : ComposeFragment() {

  private val viewModel: AddAllowedMembersViewModel by viewModels(factoryProducer = { AddAllowedMembersViewModel.Factory(profileId) })
  private val lifecycleDisposable = LifecycleDisposable()
  private val profileId: Long by lazy { AddAllowedMembersFragmentArgs.fromBundle(requireArguments()).profileId }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    lifecycleDisposable.bindTo(viewLifecycleOwner.lifecycle)
  }

  @Composable
  override fun FragmentContent() {
    val state by remember { viewModel.getProfile().map { GetProfileResult.Ready(it) }.asFlow() }
      .collectAsStateWithLifecycle(GetProfileResult.Loading)
    val callbacks = remember { Callbacks() }

    if (state is GetProfileResult.Ready) {
      AddAllowedMembersContent(
        state = (state as GetProfileResult.Ready).notificationProfileAndRecipients,
        callbacks = callbacks
      )
    }
  }

  private fun undoRemove(id: RecipientId) {
    lifecycleDisposable += viewModel.addMember(id)
      .subscribe()
  }

  companion object {
    private val TAG = Log.tag(AddAllowedMembersFragment::class.java)
  }

  private inner class Callbacks : AddAllowedMembersCallbacks {
    override fun onNavigationClick() {
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onAllowAllCallsChanged(enabled: Boolean) {
      lifecycleDisposable += viewModel.toggleAllowAllCalls()
        .subscribeBy(
          onError = { Log.w(TAG, "Error updating profile", it) }
        )
    }

    override fun onNotifyForAllMentionsChanged(enabled: Boolean) {
      lifecycleDisposable += viewModel.toggleAllowAllMentions()
        .subscribeBy(
          onError = { Log.w(TAG, "Error updating profile", it) }
        )
    }

    override fun onAddMembersClick(id: Long, allowedMembers: Set<RecipientId>) {
      findNavController().safeNavigate(
        AddAllowedMembersFragmentDirections.actionAddAllowedMembersFragmentToSelectRecipientsFragment(id)
          .setCurrentSelection(allowedMembers.toTypedArray())
      )
    }

    override fun onRemoveMemberClick(id: RecipientId) {
      lifecycleDisposable += viewModel.removeMember(id)
        .subscribeBy(
          onSuccess = { removed ->
            view?.let { view ->
              Snackbar.make(view, getString(R.string.NotificationProfileDetails__s_removed, removed.getDisplayName(requireContext())), Snackbar.LENGTH_LONG)
                .setAction(R.string.NotificationProfileDetails__undo) { undoRemove(id) }
                .show()
            }
          }
        )
    }

    override fun onNextClick() {
      findNavController().safeNavigate(AddAllowedMembersFragmentDirections.actionAddAllowedMembersFragmentToEditNotificationProfileScheduleFragment(profileId, true))
    }
  }
}

private sealed interface GetProfileResult {
  data object Loading : GetProfileResult
  data class Ready(val notificationProfileAndRecipients: NotificationProfileAndRecipients) : GetProfileResult
}

private interface AddAllowedMembersCallbacks {
  fun onNavigationClick() = Unit
  fun onAllowAllCallsChanged(enabled: Boolean) = Unit
  fun onNotifyForAllMentionsChanged(enabled: Boolean) = Unit
  fun onAddMembersClick(id: Long, allowedMembers: Set<RecipientId>) = Unit
  fun onRemoveMemberClick(id: RecipientId) = Unit
  fun onNextClick() = Unit

  object Empty : AddAllowedMembersCallbacks
}

@Composable
private fun AddAllowedMembersContent(
  state: NotificationProfileAndRecipients,
  callbacks: AddAllowedMembersCallbacks,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
  Scaffolds.Settings(
    title = "",
    onNavigationClick = callbacks::onNavigationClick,
    navigationIcon = ZonaRosaIcons.ArrowStart.imageVector,
    snackbarHost = {
      Snackbars.Host(snackbarHostState)
    }
  ) { contentPadding ->
    Column(
      modifier = Modifier.padding(contentPadding)
    ) {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .then(rememberStatusBarColorNestedScrollModifier())
      ) {
        item {
          Text(
            text = stringResource(R.string.AddAllowedMembers__allowed_notifications),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .horizontalGutters()
              .padding(top = 20.dp)
              .fillMaxWidth()
          )

          Text(
            text = stringResource(R.string.AddAllowedMembers__add_people_and_groups_you_want_notifications_and_calls_from_when_this_profile_is_on),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .horizontalGutters()
              .padding(top = 12.dp, bottom = 24.dp)
              .fillMaxWidth()
          )
        }

        item {
          Texts.SectionHeader(
            text = stringResource(R.string.AddAllowedMembers__allowed_notifications)
          )
        }

        item {
          val callback = remember(state.profile.id, state.profile.allowedMembers) {
            {
              callbacks.onAddMembersClick(state.profile.id, state.profile.allowedMembers)
            }
          }

          NotificationProfileAddMembers(onClick = callback)
        }

        for (member in state.recipients) {
          item(key = member.id) {
            NotificationProfileRecipient(
              recipient = member,
              onRemoveClick = callbacks::onRemoveMemberClick
            )
          }
        }

        item {
          Texts.SectionHeader(
            text = stringResource(R.string.AddAllowedMembers__exceptions)
          )
        }

        item {
          Rows.ToggleRow(
            checked = state.profile.allowAllCalls,
            text = stringResource(R.string.AddAllowedMembers__allow_all_calls),
            icon = ZonaRosaIcons.Phone.imageVector,
            onCheckChanged = callbacks::onAllowAllCallsChanged
          )
        }

        item {
          Rows.ToggleRow(
            checked = state.profile.allowAllMentions,
            text = stringResource(R.string.AddAllowedMembers__notify_for_all_mentions),
            icon = ZonaRosaIcons.At.imageVector,
            onCheckChanged = callbacks::onNotifyForAllMentionsChanged
          )
        }
      }

      Buttons.LargeTonal(
        onClick = callbacks::onNextClick,
        modifier = Modifier
          .align(Alignment.End)
          .padding(16.dp)
      ) {
        Text(text = stringResource(R.string.EditNotificationProfileFragment__next))
      }
    }
  }
}

@DayNightPreviews
@Composable
private fun AddAllowedMembersContentPreview() {
  Previews.Preview {
    AddAllowedMembersContent(
      state = NotificationProfileAndRecipients(
        profile = NotificationProfile(
          id = 0L,
          name = "Test Profile",
          emoji = EmojiStrings.PHOTO,
          createdAt = System.currentTimeMillis(),
          schedule = NotificationProfileSchedule(
            id = 0L
          ),
          notificationProfileId = NotificationProfileId(UUID.randomUUID())
        ),
        recipients = (1..3).map {
          Recipient(
            id = RecipientId.from(it.toLong()),
            isResolving = false,
            registeredValue = RecipientTable.RegisteredState.REGISTERED,
            systemContactName = "Test User $it"
          )
        }
      ),
      callbacks = AddAllowedMembersCallbacks.Empty
    )
  }
}
