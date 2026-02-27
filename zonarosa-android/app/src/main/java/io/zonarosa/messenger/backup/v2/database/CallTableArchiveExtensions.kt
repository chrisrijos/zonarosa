/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.database

import io.zonarosa.core.util.select
import io.zonarosa.messenger.database.CallTable

fun CallTable.getAdhocCallsForBackup(): AdHocCallArchiveExporter {
  return AdHocCallArchiveExporter(
    readableDatabase
      .select()
      .from(CallTable.TABLE_NAME)
      .where("${CallTable.TYPE} = ?", CallTable.Type.serialize(CallTable.Type.AD_HOC_CALL))
      .run()
  )
}
