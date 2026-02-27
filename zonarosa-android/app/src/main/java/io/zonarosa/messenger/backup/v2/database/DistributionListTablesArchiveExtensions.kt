/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.database

import io.zonarosa.core.util.select
import io.zonarosa.core.util.withinTransaction
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.exporters.DistributionListArchiveExporter
import io.zonarosa.messenger.database.DistributionListTables
import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.database.model.DistributionListPrivacyMode
import io.zonarosa.messenger.recipients.RecipientId

fun DistributionListTables.getAllForBackup(selfRecipientId: RecipientId, exportState: ExportState): DistributionListArchiveExporter {
  val cursor = readableDatabase
    .select()
    .from(DistributionListTables.ListTable.TABLE_NAME)
    .run()

  return DistributionListArchiveExporter(cursor, this, selfRecipientId, exportState)
}

fun DistributionListTables.getMembersForBackup(id: DistributionListId): List<RecipientId> {
  lateinit var privacyMode: DistributionListPrivacyMode
  lateinit var rawMembers: List<RecipientId>

  readableDatabase.withinTransaction {
    privacyMode = getPrivacyMode(id)
    rawMembers = getRawMembers(id, privacyMode)
  }

  return when (privacyMode) {
    DistributionListPrivacyMode.ALL -> emptyList()
    DistributionListPrivacyMode.ONLY_WITH -> rawMembers
    DistributionListPrivacyMode.ALL_EXCEPT -> rawMembers
  }
}
