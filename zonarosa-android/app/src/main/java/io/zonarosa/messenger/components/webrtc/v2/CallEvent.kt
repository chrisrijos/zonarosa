/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

import android.content.Context
import io.zonarosa.messenger.R
import io.zonarosa.messenger.database.model.IdentityRecord
import io.zonarosa.messenger.recipients.Recipient

/**
 * Replacement sealed class for WebRtcCallViewModel.Event
 */
sealed interface CallEvent {
  data object ShowVideoTooltip : CallEvent
  data object DismissVideoTooltip : CallEvent
  data object ShowWifiToCellularPopup : CallEvent
  data object ShowSwitchCameraTooltip : CallEvent
  data object DismissSwitchCameraTooltip : CallEvent
  data class StartCall(val isVideoCall: Boolean) : CallEvent
  data class ShowGroupCallSafetyNumberChange(val identityRecords: List<IdentityRecord>) : CallEvent
  data object SwitchToSpeaker : CallEvent
  data object ShowSwipeToSpeakerHint : CallEvent
  data object ShowLargeGroupAutoMuteToast : CallEvent
  data class ShowRemoteMuteToast(private val muted: Recipient, private val mutedBy: Recipient) : CallEvent {
    fun getDescription(context: Context): String {
      return if (muted.isSelf && mutedBy.isSelf) {
        context.getString(R.string.WebRtcCallView__you_muted_yourself)
      } else if (muted.isSelf) {
        context.getString(R.string.WebRtcCallView__s_remotely_muted_you, mutedBy.getDisplayName(context))
      } else if (mutedBy.isSelf) {
        context.getString(R.string.WebRtcCallView__you_remotely_muted_s, muted.getDisplayName(context))
      } else {
        context.getString(R.string.WebRtcCallView__s_remotely_muted_s, mutedBy.getDisplayName(context), muted.getDisplayName(context))
      }
    }
  }
}
