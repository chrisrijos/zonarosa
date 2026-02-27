/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.identity.IdentityRecordList
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.ProfileKeySendJob
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

/**
 * In-Memory Cache that keeps track of whom we've sent profile keys to. This is
 * something that really only needs to happen once so that profile information is
 * displayed correctly, so we maintain an application process scoped cache.
 */
object CallLinkProfileKeySender {
  private val TAG = Log.tag(CallLinkProfileKeySender::class.java)

  private val cache = hashSetOf<RecipientId>()

  /**
   * Invoked after pressing "Join" or "Continue" on the safety number change dialog before
   * or during a call.
   */
  @JvmStatic
  fun onSendAnyway(recipientIds: Set<RecipientId>) {
    val toSendMessagesTo: Set<RecipientId> = recipientIds - cache
    cache += recipientIds

    if (toSendMessagesTo.isNotEmpty()) {
      Log.i(TAG, "Sending profile key to $toSendMessagesTo users.")
      val job = ProfileKeySendJob.createForCallLinks(toSendMessagesTo.toList())
      AppDependencies.jobManager.add(job)
    } else {
      Log.i(TAG, "No users to send profile key to.")
    }
  }

  /**
   * Given the set of recipients, for each unblocked recipient we don't distrust, send a NullMessage
   */
  fun onRecipientsUpdated(recipients: Set<Recipient>) {
    val nonBlockedNonSelfRecipients: List<Recipient> = recipients.filterNot { it.isBlocked || it.isSelf }

    val identityRecords: IdentityRecordList = AppDependencies
      .protocolStore
      .aci()
      .identities()
      .getIdentityRecords(nonBlockedNonSelfRecipients)

    val untrustedAndUnverifiedRecipients = if (identityRecords.isUntrusted(false) || identityRecords.isUnverified(false)) {
      (identityRecords.untrustedRecipients + identityRecords.unverifiedRecipients).toSet()
    } else {
      emptySet()
    }

    val trustedRecipients: Set<RecipientId> = (nonBlockedNonSelfRecipients - untrustedAndUnverifiedRecipients).map { it.id }.toSet()

    onSendAnyway(trustedRecipients)
  }
}
