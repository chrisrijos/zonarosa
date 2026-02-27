/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc.links

/**
 * Result type for call link updates.
 */
sealed interface UpdateCallLinkResult {
  data class Update(
    val state: ZonaRosaCallLinkState
  ) : UpdateCallLinkResult

  data class Delete(
    val roomId: CallLinkRoomId
  ) : UpdateCallLinkResult

  data class Failure(
    val status: Short
  ) : UpdateCallLinkResult

  /**
   * Occurs when a user tries to delete a call link that
   * the call server believes is currently being utilized.
   */
  data object CallLinkIsInUse : UpdateCallLinkResult

  data object NotAuthorized : UpdateCallLinkResult
}
