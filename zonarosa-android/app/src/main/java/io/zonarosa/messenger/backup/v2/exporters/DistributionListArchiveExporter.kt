/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.exporters

import android.database.Cursor
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.requireBoolean
import io.zonarosa.core.util.requireLong
import io.zonarosa.core.util.requireNonNullString
import io.zonarosa.core.util.requireObject
import io.zonarosa.core.util.toByteArray
import io.zonarosa.messenger.backup.v2.ArchiveRecipient
import io.zonarosa.messenger.backup.v2.ExportOddities
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.database.getMembersForBackup
import io.zonarosa.messenger.backup.v2.proto.DistributionList
import io.zonarosa.messenger.backup.v2.proto.DistributionListItem
import io.zonarosa.messenger.backup.v2.util.clampToValidBackupRange
import io.zonarosa.messenger.database.DistributionListTables
import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.database.model.DistributionListPrivacyMode
import io.zonarosa.messenger.database.model.DistributionListRecord
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.api.push.DistributionId
import java.io.Closeable

private val TAG = Log.tag(DistributionListArchiveExporter::class)

class DistributionListArchiveExporter(
  private val cursor: Cursor,
  private val distributionListTables: DistributionListTables,
  private val selfRecipientId: RecipientId,
  private val exportState: ExportState
) : Iterator<ArchiveRecipient>, Closeable {

  override fun hasNext(): Boolean {
    return cursor.count > 0 && !cursor.isLast
  }

  override fun next(): ArchiveRecipient {
    if (!cursor.moveToNext()) {
      throw NoSuchElementException()
    }

    val id: DistributionListId = DistributionListId.from(cursor.requireLong(DistributionListTables.ListTable.ID))
    val privacyMode: DistributionListPrivacyMode = cursor.requireObject(DistributionListTables.ListTable.PRIVACY_MODE, DistributionListPrivacyMode.Serializer)
    val recipientId: RecipientId = RecipientId.from(cursor.requireLong(DistributionListTables.ListTable.RECIPIENT_ID))

    val record = DistributionListRecord(
      id = id,
      name = cursor.requireNonNullString(DistributionListTables.ListTable.NAME),
      distributionId = DistributionId.from(cursor.requireNonNullString(DistributionListTables.ListTable.DISTRIBUTION_ID)),
      allowsReplies = cursor.requireBoolean(DistributionListTables.ListTable.ALLOWS_REPLIES),
      rawMembers = distributionListTables.getRawMembers(id, privacyMode),
      members = distributionListTables.getMembersForBackup(id),
      deletedAtTimestamp = cursor.requireLong(DistributionListTables.ListTable.DELETION_TIMESTAMP).clampToValidBackupRange(),
      isUnknown = cursor.requireBoolean(DistributionListTables.ListTable.IS_UNKNOWN),
      privacyMode = privacyMode
    )

    val distributionListItem = if (record.deletedAtTimestamp != 0L) {
      DistributionListItem(
        distributionId = record.distributionId.asUuid().toByteArray().toByteString(),
        deletionTimestamp = record.deletedAtTimestamp
      )
    } else {
      val members = record.members.toRemoteMemberList(selfRecipientId, exportState)
      DistributionListItem(
        distributionId = record.distributionId.asUuid().toByteArray().toByteString(),
        distributionList = DistributionList(
          name = record.name,
          allowReplies = record.allowsReplies,
          privacyMode = record.privacyMode.toBackupPrivacyMode(members.size),
          memberRecipientIds = members
        )
      )
    }

    return ArchiveRecipient(
      id = recipientId.toLong(),
      distributionList = distributionListItem
    )
  }

  override fun close() {
    cursor.close()
  }
}

private fun DistributionListPrivacyMode.toBackupPrivacyMode(memberCount: Int): DistributionList.PrivacyMode {
  return when (this) {
    DistributionListPrivacyMode.ONLY_WITH -> DistributionList.PrivacyMode.ONLY_WITH
    DistributionListPrivacyMode.ALL -> DistributionList.PrivacyMode.ALL
    DistributionListPrivacyMode.ALL_EXCEPT -> {
      if (memberCount > 0) {
        DistributionList.PrivacyMode.ALL_EXCEPT
      } else {
        Log.w(TAG, ExportOddities.distributionListAllExceptWithNoMembers())
        DistributionList.PrivacyMode.ALL
      }
    }
  }
}

private fun List<RecipientId>.toRemoteMemberList(selfRecipientId: RecipientId, exportState: ExportState): List<Long> {
  val filtered = this.filter { it != selfRecipientId }.map { it.toLong() }
  if (filtered.size != this.size) {
    Log.w(TAG, ExportOddities.distributionListHadSelfAsMember())
  }

  return filtered.filter { exportState.recipientIdToAci[it] != null || exportState.recipientIdToE164[it] != null }
}
