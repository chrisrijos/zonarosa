/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links.create

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.zonarosa.core.ui.compose.BottomSheets
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.ComposeBottomSheetDialogFragment
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Dividers
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.Rows
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.logging.Log
import io.zonarosa.ringrtc.CallLinkState
import io.zonarosa.messenger.R
import io.zonarosa.messenger.calls.YouAreAlreadyInACallSnackbar.YouAreAlreadyInACallSnackbar
import io.zonarosa.messenger.calls.links.CallLinks
import io.zonarosa.messenger.calls.links.EditCallLinkNameDialogFragment
import io.zonarosa.messenger.calls.links.ZonaRosaCallRow
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.service.webrtc.links.CreateCallLinkResult
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkState
import io.zonarosa.messenger.service.webrtc.links.UpdateCallLinkResult
import io.zonarosa.messenger.sharing.v2.ShareActivity
import io.zonarosa.messenger.util.CommunicationActions
import java.time.Instant
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Bottom sheet for creating call links
 */
class CreateCallLinkBottomSheetDialogFragment : ComposeBottomSheetDialogFragment() {

  companion object {
    private val TAG = Log.tag(CreateCallLinkBottomSheetDialogFragment::class.java)
  }

  private val viewModel: CreateCallLinkViewModel by viewModels()
  private val lifecycleDisposable = LifecycleDisposable()

  override val peekHeightPercentage: Float = 1f

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    lifecycleDisposable.bindTo(viewLifecycleOwner)
    parentFragmentManager.setFragmentResultListener(EditCallLinkNameDialogFragment.RESULT_KEY, viewLifecycleOwner) { resultKey, bundle ->
      if (bundle.containsKey(resultKey)) {
        setCallName(bundle.getString(resultKey)!!)
      }
    }
  }

  @Composable
  override fun SheetContent() {
    val callLink: CallLinkTable.CallLink by viewModel.callLink
    val displayAlreadyInACallSnackbar: Boolean by viewModel.showAlreadyInACall.collectAsStateWithLifecycle(false)
    val isLoadingAdminApprovalChange: Boolean by viewModel.isLoadingAdminApprovalChange.collectAsStateWithLifecycle(false)

    CreateCallLinkBottomSheetContent(
      callLink = callLink,
      onJoinClicked = this@CreateCallLinkBottomSheetDialogFragment::onJoinClicked,
      onAddACallNameClicked = this@CreateCallLinkBottomSheetDialogFragment::onAddACallNameClicked,
      onApproveAllMembersChanged = this@CreateCallLinkBottomSheetDialogFragment::setApproveAllMembers,
      onShareViaZonaRosaClicked = this@CreateCallLinkBottomSheetDialogFragment::onShareViaZonaRosaClicked,
      onCopyLinkClicked = this@CreateCallLinkBottomSheetDialogFragment::onCopyLinkClicked,
      onShareLinkClicked = this@CreateCallLinkBottomSheetDialogFragment::onShareLinkClicked,
      onDoneClicked = this@CreateCallLinkBottomSheetDialogFragment::onDoneClicked,
      displayAlreadyInACallSnackbar = displayAlreadyInACallSnackbar,
      isLoadingAdminApprovalChange = isLoadingAdminApprovalChange
    )
  }

  private fun setCallName(callName: String) {
    lifecycleDisposable += viewModel.setCallName(callName).subscribeBy(onSuccess = {
      if (it !is UpdateCallLinkResult.Update) {
        Log.w(TAG, "Failed to update call link name")
        toastFailure()
      }
    }, onError = this::handleError)
  }

  private fun setApproveAllMembers(approveAllMembers: Boolean) {
    lifecycleDisposable += viewModel.setApproveAllMembers(approveAllMembers).subscribeBy(onSuccess = {
      if (it !is UpdateCallLinkResult.Update) {
        Log.w(TAG, "Failed to update call link restrictions")
        toastFailure()
      }
    }, onError = this::handleError)
  }

  private fun onAddACallNameClicked() {
    val snapshot = viewModel.callLink.value
    EditCallLinkNameDialogFragment().apply {
      arguments = bundleOf(EditCallLinkNameDialogFragment.ARG_NAME to snapshot.state.name)
    }.show(parentFragmentManager, null)
  }

  private fun onJoinClicked() {
    lifecycleDisposable += viewModel.commitCallLink().subscribeBy(onSuccess = {
      when (it) {
        is EnsureCallLinkCreatedResult.Success -> {
          CommunicationActions.startVideoCall(requireActivity(), it.recipient) {
            viewModel.setShowAlreadyInACall(true)
          }
          dismissAllowingStateLoss()
        }

        is EnsureCallLinkCreatedResult.Failure -> handleCreateCallLinkFailure(it.failure)
      }
    }, onError = this::handleError)
  }

  private fun onDoneClicked() {
    lifecycleDisposable += viewModel.commitCallLink().subscribeBy(onSuccess = {
      when (it) {
        is EnsureCallLinkCreatedResult.Success -> dismissAllowingStateLoss()
        is EnsureCallLinkCreatedResult.Failure -> handleCreateCallLinkFailure(it.failure)
      }
    }, onError = this::handleError)
  }

  private fun onShareViaZonaRosaClicked() {
    lifecycleDisposable += viewModel.commitCallLink().subscribeBy(onSuccess = {
      when (it) {
        is EnsureCallLinkCreatedResult.Success -> {
          startActivity(
            ShareActivity.sendSimpleText(
              requireContext(),
              getString(R.string.CreateCallLink__use_this_link_to_join_a_zonarosa_call, CallLinks.url(viewModel.linkKeyBytes))
            )
          )
        }

        is EnsureCallLinkCreatedResult.Failure -> handleCreateCallLinkFailure(it.failure)
      }
    }, onError = this::handleError)
  }

  private fun onCopyLinkClicked() {
    lifecycleDisposable += viewModel.commitCallLink().subscribeBy(onSuccess = {
      when (it) {
        is EnsureCallLinkCreatedResult.Success -> {
          Util.copyToClipboard(requireContext(), CallLinks.url(viewModel.linkKeyBytes))
          Toast.makeText(requireContext(), R.string.CreateCallLinkBottomSheetDialogFragment__copied_to_clipboard, Toast.LENGTH_LONG).show()
        }

        is EnsureCallLinkCreatedResult.Failure -> handleCreateCallLinkFailure(it.failure)
      }
    }, onError = this::handleError)
  }

  private fun onShareLinkClicked() {
    lifecycleDisposable += viewModel.commitCallLink().subscribeBy {
      when (it) {
        is EnsureCallLinkCreatedResult.Success -> {
          val mimeType = Intent.normalizeMimeType("text/plain")
          val shareIntent = ShareCompat.IntentBuilder(requireContext())
            .setText(CallLinks.url(viewModel.linkKeyBytes))
            .setType(mimeType)
            .createChooserIntent()

          try {
            startActivity(shareIntent)
          } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.CreateCallLinkBottomSheetDialogFragment__failed_to_open_share_sheet, Toast.LENGTH_LONG).show()
          }
        }

        is EnsureCallLinkCreatedResult.Failure -> {
          Log.w(TAG, "Failed to create link: $it")
          toastFailure()
        }
      }
    }
  }

  private fun handleCreateCallLinkFailure(failure: CreateCallLinkResult.Failure) {
    Log.w(TAG, "Failed to create call link: $failure")
    toastFailure()
  }

  private fun handleError(throwable: Throwable) {
    Log.w(TAG, "Failed to create call link.", throwable)
    toastFailure()
  }

  private fun toastFailure() {
    Toast.makeText(requireContext(), R.string.CallLinkDetailsFragment__couldnt_save_changes, Toast.LENGTH_LONG).show()
  }
}

@Composable
private fun CreateCallLinkBottomSheetContent(
  callLink: CallLinkTable.CallLink,
  displayAlreadyInACallSnackbar: Boolean,
  isLoadingAdminApprovalChange: Boolean,
  onJoinClicked: () -> Unit = {},
  onAddACallNameClicked: () -> Unit = {},
  onApproveAllMembersChanged: (Boolean) -> Unit = {},
  onShareViaZonaRosaClicked: () -> Unit = {},
  onCopyLinkClicked: () -> Unit = {},
  onShareLinkClicked: () -> Unit = {},
  onDoneClicked: () -> Unit = {}
) {
  Box {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentSize(Alignment.Center)
        .verticalScroll(rememberScrollState())
    ) {
      BottomSheets.Handle(modifier = Modifier.align(Alignment.CenterHorizontally))

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__create_call_link),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(24.dp))

      ZonaRosaCallRow(
        callLink = callLink,
        callLinkPeekInfo = null,
        onJoinClicked = onJoinClicked
      )

      Spacer(modifier = Modifier.height(12.dp))

      Rows.TextRow(
        text = stringResource(
          id = if (callLink.state.name.isEmpty()) {
            R.string.CreateCallLinkBottomSheetDialogFragment__add_call_name
          } else {
            R.string.CreateCallLinkBottomSheetDialogFragment__edit_call_name
          }
        ),
        onClick = onAddACallNameClicked
      )

      Rows.ToggleRow(
        checked = callLink.state.restrictions == CallLinkState.Restrictions.ADMIN_APPROVAL,
        text = stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__require_admin_approval),
        onCheckChanged = onApproveAllMembersChanged,
        isLoading = isLoadingAdminApprovalChange
      )

      Dividers.Default()

      Rows.TextRow(
        text = stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__share_link_via_zonarosa),
        icon = ZonaRosaIcons.Forward.imageVector,
        onClick = onShareViaZonaRosaClicked
      )

      Rows.TextRow(
        text = stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__copy_link),
        icon = ZonaRosaIcons.Copy.imageVector,
        onClick = onCopyLinkClicked
      )

      Rows.TextRow(
        text = stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__share_link),
        icon = ZonaRosaIcons.Share.imageVector,
        onClick = onShareLinkClicked
      )

      Buttons.MediumTonal(
        onClick = onDoneClicked,
        modifier = Modifier
          .padding(end = dimensionResource(id = CoreUiR.dimen.gutter))
          .align(Alignment.End)
      ) {
        Text(text = stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__done))
      }

      Spacer(modifier = Modifier.size(16.dp))
    }

    YouAreAlreadyInACallSnackbar(
      displaySnackbar = displayAlreadyInACallSnackbar,
      modifier = Modifier.align(Alignment.BottomCenter)
    )
  }
}

@DayNightPreviews
@Composable
private fun CreateCallLinkBottomSheetContentPreview() {
  Previews.BottomSheetContentPreview {
    CreateCallLinkBottomSheetContent(
      callLink = CallLinkTable.CallLink(
        recipientId = RecipientId.UNKNOWN,
        roomId = CallLinkRoomId.fromBytes(byteArrayOf(1, 2, 3, 4)),
        credentials = null,
        state = ZonaRosaCallLinkState(
          name = "Test Call",
          restrictions = CallLinkState.Restrictions.ADMIN_APPROVAL,
          revoked = false,
          expiration = Instant.MAX
        ),
        deletionTimestamp = 0L
      ),
      displayAlreadyInACallSnackbar = true,
      isLoadingAdminApprovalChange = false
    )
  }
}
