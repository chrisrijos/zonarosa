/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.processor

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.ExportSkips
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.ImportState
import io.zonarosa.messenger.backup.v2.database.getAdhocCallsForBackup
import io.zonarosa.messenger.backup.v2.importer.AdHodCallArchiveImporter
import io.zonarosa.messenger.backup.v2.proto.AdHocCall
import io.zonarosa.messenger.backup.v2.proto.Frame
import io.zonarosa.messenger.backup.v2.stream.BackupFrameEmitter
import io.zonarosa.messenger.database.ZonaRosaDatabase

/**
 * Handles importing/exporting [AdHocCall] frames for an archive.
 */
object AdHocCallArchiveProcessor {

  val TAG = Log.tag(AdHocCallArchiveProcessor::class.java)

  fun export(db: ZonaRosaDatabase, exportState: ExportState, emitter: BackupFrameEmitter) {
    db.callTable.getAdhocCallsForBackup().use { reader ->
      for (callLog in reader) {
        if (exportState.recipientIds.contains(callLog.recipientId)) {
          emitter.emit(Frame(adHocCall = callLog))
        } else {
          Log.w(TAG, ExportSkips.callWithMissingRecipient(callLog.callTimestamp))
        }
      }
    }
  }

  fun import(call: AdHocCall, importState: ImportState) {
    AdHodCallArchiveImporter.import(call, importState)
  }
}
