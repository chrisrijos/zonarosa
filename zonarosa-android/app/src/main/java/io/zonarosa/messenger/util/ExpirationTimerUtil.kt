/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.util

import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.RecipientId

/**
 * This exists as a temporary shim to improve the callsites where we'll be setting the expiration timer.
 *
 * Until the versions that don't understand expiration timers expire, we'll have to check capabilities before incrementing the version.
 *
 * After those old clients expire, we can remove this shim entirely and call the RecipientTable methods directly.
 */
object ExpirationTimerUtil {

  @JvmStatic
  fun setExpirationTimer(recipientId: RecipientId, expirationTimeSeconds: Int): Int {
    val selfCapable = true
    val recipientCapable = true

    return if (selfCapable && recipientCapable) {
      ZonaRosaDatabase.recipients.setExpireMessagesAndIncrementVersion(recipientId, expirationTimeSeconds)
    } else {
      ZonaRosaDatabase.recipients.setExpireMessagesWithoutIncrementingVersion(recipientId, expirationTimeSeconds)
      1
    }
  }
}
