/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.model

import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendEndorsement
import io.zonarosa.messenger.recipients.RecipientId

/**
 * Contains the individual group send endorsements for a specific group
 * source from our local db.
 */
data class GroupSendEndorsementRecords(val endorsements: Map<RecipientId, GroupSendEndorsement?>) {
  fun getEndorsement(recipientId: RecipientId): GroupSendEndorsement? {
    return endorsements[recipientId]
  }

  fun isMissingAnyEndorsements(): Boolean {
    return endorsements.values.any { it == null }
  }
}
