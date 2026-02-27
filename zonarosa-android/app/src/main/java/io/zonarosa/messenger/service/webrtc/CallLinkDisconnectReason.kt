/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc

/**
 * Describes why a user was not able to join a call link.
 *
 * Note: postedAt is kept as a long to ensure Java compatibility.
 */
sealed interface CallLinkDisconnectReason {
  val postedAt: Long

  data class RemovedFromCall(override val postedAt: Long = System.currentTimeMillis()) : CallLinkDisconnectReason
  data class DeniedRequestToJoinCall(override val postedAt: Long = System.currentTimeMillis()) : CallLinkDisconnectReason
}
