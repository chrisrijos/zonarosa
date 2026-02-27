/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc.links

/**
 * Result type for call link creation.
 */
sealed interface CreateCallLinkResult {
  data class Success(
    val credentials: CallLinkCredentials,
    val state: ZonaRosaCallLinkState
  ) : CreateCallLinkResult

  data class Failure(
    val status: Short
  ) : CreateCallLinkResult
}
