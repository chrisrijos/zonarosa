/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.storage

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.isNotEmpty
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.toOptional
import io.zonarosa.ringrtc.CallLinkRootKey
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.service.api.storage.ZonaRosaCallLinkRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.storage.toZonaRosaCallLinkRecord
import java.util.Optional

/**
 * Record processor for [ZonaRosaCallLinkRecord].
 * Handles merging and updating our local store when processing remote call link storage records.
 */
class CallLinkRecordProcessor : DefaultStorageRecordProcessor<ZonaRosaCallLinkRecord>() {

  companion object {
    private val TAG = Log.tag(CallLinkRecordProcessor::class)
  }

  override fun compare(o1: ZonaRosaCallLinkRecord?, o2: ZonaRosaCallLinkRecord?): Int {
    return if (o1?.proto?.rootKey == o2?.proto?.rootKey) {
      0
    } else {
      1
    }
  }

  override fun isInvalid(remote: ZonaRosaCallLinkRecord): Boolean {
    return remote.proto.adminPasskey.isNotEmpty() && remote.proto.deletedAtTimestampMs > 0L
  }

  override fun getMatching(remote: ZonaRosaCallLinkRecord, keyGenerator: StorageKeyGenerator): Optional<ZonaRosaCallLinkRecord> {
    Log.d(TAG, "Attempting to get matching record...")
    val callRootKey = CallLinkRootKey(remote.proto.rootKey.toByteArray())
    val roomId = CallLinkRoomId.fromCallLinkRootKey(callRootKey)
    val callLink = ZonaRosaDatabase.callLinks.getCallLinkByRoomId(roomId)

    if (callLink != null && callLink.credentials?.adminPassBytes != null) {
      return ZonaRosaCallLinkRecord.newBuilder(null).apply {
        rootKey = callRootKey.keyBytes.toByteString()
        adminPasskey = callLink.credentials.adminPassBytes.toByteString()
        deletedAtTimestampMs = callLink.deletionTimestamp
      }.build().toZonaRosaCallLinkRecord(StorageId.forCallLink(keyGenerator.generate())).toOptional()
    } else {
      return Optional.empty<ZonaRosaCallLinkRecord>()
    }
  }

  /**
   * A deleted record takes precedence over a non-deleted record
   * An earlier deletion takes precedence over a later deletion
   * Other fields should not change, except for the clearing of the admin passkey on deletion
   */
  override fun merge(remote: ZonaRosaCallLinkRecord, local: ZonaRosaCallLinkRecord, keyGenerator: StorageKeyGenerator): ZonaRosaCallLinkRecord {
    return if (remote.proto.deletedAtTimestampMs > 0 && local.proto.deletedAtTimestampMs > 0) {
      if (remote.proto.deletedAtTimestampMs < local.proto.deletedAtTimestampMs) {
        remote
      } else {
        local
      }
    } else if (remote.proto.deletedAtTimestampMs > 0) {
      remote
    } else if (local.proto.deletedAtTimestampMs > 0) {
      local
    } else {
      remote
    }
  }

  override fun insertLocal(record: ZonaRosaCallLinkRecord) {
    insertOrUpdateRecord(record)
  }

  override fun updateLocal(update: StorageRecordUpdate<ZonaRosaCallLinkRecord>) {
    insertOrUpdateRecord(update.new)
  }

  private fun insertOrUpdateRecord(record: ZonaRosaCallLinkRecord) {
    val rootKey = CallLinkRootKey(record.proto.rootKey.toByteArray())

    ZonaRosaDatabase.callLinks.insertOrUpdateCallLinkByRootKey(
      callLinkRootKey = rootKey,
      adminPassKey = record.proto.adminPasskey.toByteArray(),
      deletionTimestamp = record.proto.deletedAtTimestampMs,
      storageId = record.id
    )
  }
}
