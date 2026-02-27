/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc.links

/**
 * Result type for call link reads.
 */
sealed interface ReadCallLinkResult {
  data class Success(
    val callLinkState: ZonaRosaCallLinkState
  ) : ReadCallLinkResult

  data class Failure(val status: Short) : ReadCallLinkResult
}
