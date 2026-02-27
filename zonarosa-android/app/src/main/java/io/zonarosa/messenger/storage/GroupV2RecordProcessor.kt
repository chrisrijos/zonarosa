package io.zonarosa.messenger.storage

import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey
import io.zonarosa.messenger.database.GroupTable
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.RecipientRecord
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.service.api.storage.ZonaRosaGroupV2Record
import io.zonarosa.service.api.storage.ZonaRosaStorageRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.api.storage.toZonaRosaGroupV2Record
import java.util.Optional

/**
 * Record processor for [ZonaRosaGroupV2Record].
 * Handles merging and updating our local store when processing remote gv2 storage records.
 */
class GroupV2RecordProcessor(private val recipientTable: RecipientTable, private val groupDatabase: GroupTable) : DefaultStorageRecordProcessor<ZonaRosaGroupV2Record>() {
  companion object {
    private val TAG = Log.tag(GroupV2RecordProcessor::class.java)
  }

  constructor() : this(ZonaRosaDatabase.recipients, ZonaRosaDatabase.groups)

  override fun isInvalid(remote: ZonaRosaGroupV2Record): Boolean {
    return remote.proto.masterKey.size != GroupMasterKey.SIZE
  }

  override fun getMatching(remote: ZonaRosaGroupV2Record, keyGenerator: StorageKeyGenerator): Optional<ZonaRosaGroupV2Record> {
    val groupId = GroupId.v2(GroupMasterKey(remote.proto.masterKey.toByteArray()))

    val recipientId = recipientTable.getByGroupId(groupId)

    return recipientId
      .map { recipientTable.getRecordForSync(it)!! }
      .map { settings: RecipientRecord ->
        if (settings.syncExtras.groupMasterKey != null) {
          StorageSyncModels.localToRemoteRecord(settings)
        } else {
          Log.w(TAG, "No local master key. Assuming it matches remote since the groupIds match. Enqueuing a fetch to fix the bad state.")
          groupDatabase.fixMissingMasterKey(GroupMasterKey(remote.proto.masterKey.toByteArray()))
          StorageSyncModels.localToRemoteRecord(settings, GroupMasterKey(remote.proto.masterKey.toByteArray()))
        }
      }
      .map { record: ZonaRosaStorageRecord -> record.proto.groupV2!!.toZonaRosaGroupV2Record(record.id) }
  }

  override fun merge(remote: ZonaRosaGroupV2Record, local: ZonaRosaGroupV2Record, keyGenerator: StorageKeyGenerator): ZonaRosaGroupV2Record {
    val merged = ZonaRosaGroupV2Record.newBuilder(remote.serializedUnknowns).apply {
      masterKey = remote.proto.masterKey
      blocked = remote.proto.blocked
      whitelisted = remote.proto.whitelisted
      archived = remote.proto.archived
      markedUnread = remote.proto.markedUnread
      mutedUntilTimestamp = remote.proto.mutedUntilTimestamp
      dontNotifyForMentionsIfMuted = remote.proto.dontNotifyForMentionsIfMuted
      hideStory = remote.proto.hideStory
      storySendMode = remote.proto.storySendMode
      avatarColor = if (ZonaRosaStore.account.isPrimaryDevice) local.proto.avatarColor else remote.proto.avatarColor
    }.build().toZonaRosaGroupV2Record(StorageId.forGroupV2(keyGenerator.generate()))

    val matchesRemote = doParamsMatch(remote, merged)
    val matchesLocal = doParamsMatch(local, merged)

    return if (matchesRemote) {
      remote
    } else if (matchesLocal) {
      local
    } else {
      merged
    }
  }

  override fun insertLocal(record: ZonaRosaGroupV2Record) {
    recipientTable.applyStorageSyncGroupV2Insert(record)
  }

  override fun updateLocal(update: StorageRecordUpdate<ZonaRosaGroupV2Record>) {
    recipientTable.applyStorageSyncGroupV2Update(update)
  }

  override fun compare(lhs: ZonaRosaGroupV2Record, rhs: ZonaRosaGroupV2Record): Int {
    return if (lhs.proto.masterKey == rhs.proto.masterKey) {
      0
    } else {
      1
    }
  }
}
