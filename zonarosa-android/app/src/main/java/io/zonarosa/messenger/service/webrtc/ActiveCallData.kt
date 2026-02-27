/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize
import io.zonarosa.core.util.getParcelableCompat
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState

/**
 * Active call data to be returned from calls to isInCallQuery.
 */
@Parcelize
data class ActiveCallData(
  val recipientId: RecipientId
) : Parcelable {

  companion object {
    private const val KEY = "ACTIVE_CALL_DATA"

    @JvmStatic
    fun fromCallState(webRtcServiceState: WebRtcServiceState): ActiveCallData {
      return ActiveCallData(
        webRtcServiceState.callInfoState.callRecipient.id
      )
    }

    @JvmStatic
    fun fromBundle(bundle: Bundle): ActiveCallData {
      return bundle.getParcelableCompat(KEY, ActiveCallData::class.java)!!
    }
  }

  fun toBundle(): Bundle = bundleOf(KEY to this)
}
