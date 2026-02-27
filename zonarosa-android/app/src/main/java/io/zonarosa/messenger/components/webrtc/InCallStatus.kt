/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc

/**
 * Data interface for the in-call status text to be displayed while a call
 * is ongoing.
 */
sealed interface InCallStatus {
  /**
   * The elapsed time the call has been connected for.
   */
  data class ElapsedTime(val elapsedTime: Long) : InCallStatus

  /**
   * The number of users requesting to join a call link.
   */
  data class PendingCallLinkUsers(val pendingUserCount: Int) : InCallStatus

  /**
   * The number of users in a call link.
   */
  data class JoinedCallLinkUsers(val joinedUserCount: Int) : InCallStatus
}
