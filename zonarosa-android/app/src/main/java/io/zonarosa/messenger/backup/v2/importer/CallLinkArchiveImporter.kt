/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.importer

import io.zonarosa.core.util.isEmpty
import io.zonarosa.core.util.logging.Log
import io.zonarosa.ringrtc.CallLinkRootKey
import io.zonarosa.ringrtc.CallLinkState
import io.zonarosa.messenger.backup.v2.ArchiveCallLink
import io.zonarosa.messenger.backup.v2.proto.CallLink
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkState
import java.time.Instant

/**
 * Handles the importing of [ArchiveCallLink] models into the local database.
 */
object CallLinkArchiveImporter {

  private val TAG = Log.tag(CallLinkArchiveImporter::class)

  fun import(callLink: ArchiveCallLink): RecipientId? {
    val rootKey: CallLinkRootKey = try {
      CallLinkRootKey(callLink.rootKey.toByteArray())
    } catch (e: Exception) {
      if (callLink.rootKey.isEmpty()) {
        Log.w(TAG, "Missing root key!")
      } else {
        Log.w(TAG, "Failed to parse a non-empty root key!")
      }
      return null
    }

    return ZonaRosaDatabase.callLinks.insertCallLink(
      CallLinkTable.CallLink(
        recipientId = RecipientId.UNKNOWN,
        roomId = CallLinkRoomId.fromCallLinkRootKey(rootKey),
        credentials = CallLinkCredentials(callLink.rootKey.toByteArray(), callLink.adminKey?.toByteArray()),
        state = ZonaRosaCallLinkState(
          name = callLink.name,
          restrictions = callLink.restrictions.toLocal(),
          expiration = Instant.ofEpochMilli(callLink.expirationMs)
        ),
        deletionTimestamp = 0L
      )
    )
  }
}

private fun CallLink.Restrictions.toLocal(): CallLinkState.Restrictions {
  return when (this) {
    CallLink.Restrictions.ADMIN_APPROVAL -> CallLinkState.Restrictions.ADMIN_APPROVAL
    CallLink.Restrictions.NONE -> CallLinkState.Restrictions.NONE
    CallLink.Restrictions.UNKNOWN -> CallLinkState.Restrictions.UNKNOWN
  }
}
