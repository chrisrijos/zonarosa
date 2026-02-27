/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.zonarosa.messenger.BaseActivity
import io.zonarosa.messenger.R
import io.zonarosa.messenger.calls.links.CallLinks
import io.zonarosa.messenger.calls.links.EditCallLinkNameDialogFragment
import io.zonarosa.messenger.components.webrtc.controls.CallInfoView
import io.zonarosa.messenger.components.webrtc.controls.ControlsAndInfoViewModel
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.events.CallParticipant

/**
 * Callbacks for the CallInfoView, shared between CallActivity and ControlsAndInfoController.
 */
class CallInfoCallbacks(
  private val activity: BaseActivity,
  private val controlsAndInfoViewModel: ControlsAndInfoViewModel
) : CallInfoView.Callbacks {

  override fun onShareLinkClicked() {
    val mimeType = Intent.normalizeMimeType("text/plain")
    val shareIntent = ShareCompat.IntentBuilder(activity)
      .setText(CallLinks.url(controlsAndInfoViewModel.rootKeySnapshot))
      .setType(mimeType)
      .createChooserIntent()

    try {
      activity.startActivity(shareIntent)
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(activity, R.string.CreateCallLinkBottomSheetDialogFragment__failed_to_open_share_sheet, Toast.LENGTH_LONG).show()
    }
  }

  override fun onEditNameClicked(name: String) {
    EditCallLinkNameDialogFragment().apply {
      arguments = bundleOf(EditCallLinkNameDialogFragment.ARG_NAME to name)
    }.show(activity.supportFragmentManager, null)
  }

  override fun onBlock(callParticipant: CallParticipant) {
    MaterialAlertDialogBuilder(activity)
      .setNegativeButton(android.R.string.cancel, null)
      .setMessage(activity.resources.getString(R.string.CallLinkInfoSheet__remove_s_from_the_call, callParticipant.recipient.getShortDisplayName(activity)))
      .setPositiveButton(R.string.CallLinkInfoSheet__remove) { _, _ ->
        AppDependencies.zonarosaCallManager.removeFromCallLink(callParticipant)
      }
      .setNeutralButton(R.string.CallLinkInfoSheet__block_from_call) { _, _ ->
        AppDependencies.zonarosaCallManager.blockFromCallLink(callParticipant)
      }
      .show()
  }
}
