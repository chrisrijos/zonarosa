/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.database

import io.zonarosa.core.util.select
import io.zonarosa.messenger.database.CallLinkTable

fun CallLinkTable.getCallLinksForBackup(): CallLinkArchiveExporter {
  val cursor = readableDatabase
    .select()
    .from(CallLinkTable.TABLE_NAME)
    .where("${CallLinkTable.ROOT_KEY} NOT NULL")
    .run()

  return CallLinkArchiveExporter(cursor)
}
