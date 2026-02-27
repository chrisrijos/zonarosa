/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app

import androidx.compose.runtime.Immutable
import com.google.common.base.Objects
import io.zonarosa.messenger.badges.models.Badge
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient

/**
 * Derived state class of recipient for BioRow
 */
@Immutable
class BioRecipientState(
  val recipient: Recipient
) {
  val username: String = recipient.username.orElse("")
  val featuredBadge: Badge? = recipient.featuredBadge
  val profileName: ProfileName = recipient.profileName
  val e164: String = recipient.e164.orElse("")
  val combinedAboutAndEmoji: String? = recipient.combinedAboutAndEmoji

  override fun equals(other: Any?): Boolean {
    if (other !is Recipient) return false
    return recipient.hasSameContent(other)
  }

  override fun hashCode(): Int {
    return Objects.hashCode(
      recipient,
      username,
      featuredBadge,
      profileName,
      e164,
      combinedAboutAndEmoji
    )
  }
}
