/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc.links

import io.zonarosa.ringrtc.CallLinkState.Restrictions
import java.time.Instant

/**
 * Adapter class between our app code and RingRTC CallLinkState.
 */
data class ZonaRosaCallLinkState(
  val name: String = "",
  val restrictions: Restrictions = Restrictions.UNKNOWN,
  @get:JvmName("hasBeenRevoked") val revoked: Boolean = false,
  val expiration: Instant = Instant.MAX
)
