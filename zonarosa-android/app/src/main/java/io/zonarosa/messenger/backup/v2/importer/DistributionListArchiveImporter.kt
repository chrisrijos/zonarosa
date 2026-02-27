/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.importer

import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.ImportState
import io.zonarosa.messenger.backup.v2.proto.DistributionList
import io.zonarosa.messenger.backup.v2.proto.DistributionListItem
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.DistributionListPrivacyMode
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.api.push.DistributionId

/**
 * Handles the importing of [DistributionListItem] models into the local database.
 */
object DistributionListArchiveImporter {

  private val TAG = Log.tag(DistributionListArchiveImporter.javaClass)

  fun import(dlistItem: DistributionListItem, importState: ImportState): RecipientId? {
    if (dlistItem.deletionTimestamp != null && dlistItem.deletionTimestamp > 0) {
      val dlistId = ZonaRosaDatabase.distributionLists.createList(
        name = "",
        members = emptyList(),
        distributionId = DistributionId.from(UuidUtil.fromByteString(dlistItem.distributionId)),
        allowsReplies = false,
        deletionTimestamp = dlistItem.deletionTimestamp,
        storageId = null,
        privacyMode = DistributionListPrivacyMode.ONLY_WITH
      )!!

      return ZonaRosaDatabase.distributionLists.getRecipientId(dlistId)!!
    }

    val dlist = dlistItem.distributionList ?: return null
    val members: List<RecipientId> = dlist.memberRecipientIds
      .mapNotNull { importState.remoteToLocalRecipientId[it] }

    if (members.size != dlist.memberRecipientIds.size) {
      Log.w(TAG, "Couldn't find some member recipients! Missing backup recipientIds: ${dlist.memberRecipientIds.toSet() - members.toSet()}")
    }

    val distributionId = DistributionId.from(UuidUtil.fromByteString(dlistItem.distributionId))
    val privacyMode = dlist.privacyMode.toLocalPrivacyMode()

    val dlistId = ZonaRosaDatabase.distributionLists.createList(
      name = dlist.name,
      members = members,
      distributionId = distributionId,
      allowsReplies = dlist.allowReplies,
      deletionTimestamp = dlistItem.deletionTimestamp ?: 0,
      storageId = null,
      privacyMode = privacyMode
    )!!

    return ZonaRosaDatabase.distributionLists.getRecipientId(dlistId)!!
  }
}

private fun DistributionList.PrivacyMode.toLocalPrivacyMode(): DistributionListPrivacyMode {
  return when (this) {
    DistributionList.PrivacyMode.UNKNOWN -> DistributionListPrivacyMode.ALL
    DistributionList.PrivacyMode.ONLY_WITH -> DistributionListPrivacyMode.ONLY_WITH
    DistributionList.PrivacyMode.ALL -> DistributionListPrivacyMode.ALL
    DistributionList.PrivacyMode.ALL_EXCEPT -> DistributionListPrivacyMode.ALL_EXCEPT
  }
}
