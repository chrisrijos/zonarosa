/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

import io.zonarosa.messenger.recipients.Recipient

interface PendingParticipantsListener {
  /**
   * Display the sheet containing the request for the top level participant
   */
  fun onLaunchRecipientSheet(pendingRecipient: Recipient)

  /**
   * Given recipient should be admitted to the call
   */
  fun onAllowPendingRecipient(pendingRecipient: Recipient)

  /**
   * Given recipient should be rejected from the call
   */
  fun onRejectPendingRecipient(pendingRecipient: Recipient)

  /**
   * Display the sheet containing all of the requests for the given call
   */
  fun onLaunchPendingRequestsSheet()

  object Empty : PendingParticipantsListener {
    override fun onLaunchRecipientSheet(pendingRecipient: Recipient) = Unit
    override fun onAllowPendingRecipient(pendingRecipient: Recipient) = Unit
    override fun onRejectPendingRecipient(pendingRecipient: Recipient) = Unit
    override fun onLaunchPendingRequestsSheet() = Unit
  }
}
