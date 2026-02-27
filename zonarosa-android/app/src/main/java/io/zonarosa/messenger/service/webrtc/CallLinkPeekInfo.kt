/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc

import io.zonarosa.ringrtc.CallId
import io.zonarosa.ringrtc.PeekInfo
import io.zonarosa.messenger.recipients.Recipient

/**
 * App-level peek info object for call links.
 */
data class CallLinkPeekInfo(
  val callId: CallId?,
  val isActive: Boolean,
  val isJoined: Boolean
) {

  val isCompletelyInactive
    get() = callId == null && !isActive && !isJoined

  companion object {
    @JvmStatic
    fun fromPeekInfo(peekInfo: PeekInfo): CallLinkPeekInfo {
      return CallLinkPeekInfo(
        callId = peekInfo.eraId?.let { CallId.fromEra(it) },
        isActive = peekInfo.joinedMembers.isNotEmpty(),
        isJoined = peekInfo.joinedMembers.contains(Recipient.self().requireServiceId().rawUuid)
      )
    }
  }
}
