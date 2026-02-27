/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.processor

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.ImportState
import io.zonarosa.messenger.backup.v2.database.createChatItemInserter
import io.zonarosa.messenger.backup.v2.database.getMessagesForBackup
import io.zonarosa.messenger.backup.v2.importer.ChatItemArchiveImporter
import io.zonarosa.messenger.backup.v2.proto.ChatItem
import io.zonarosa.messenger.backup.v2.proto.Frame
import io.zonarosa.messenger.backup.v2.stream.BackupFrameEmitter
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.RecipientId

/**
 * Handles importing/exporting [ChatItem] frames for an archive.
 */
object ChatItemArchiveProcessor {
  val TAG = Log.tag(ChatItemArchiveProcessor::class.java)

  fun export(db: ZonaRosaDatabase, exportState: ExportState, selfRecipientId: RecipientId, messageInclusionCutoffTime: Long, cancellationZonaRosa: () -> Boolean, emitter: BackupFrameEmitter) {
    db.messageTable.getMessagesForBackup(db, exportState.backupTime, selfRecipientId, messageInclusionCutoffTime, exportState).use { chatItems ->
      var count = 0
      while (chatItems.hasNext()) {
        if (count % 1000 == 0 && cancellationZonaRosa()) {
          return@use
        }

        val chatItem: ChatItem? = chatItems.next()
        if (chatItem != null) {
          if (exportState.threadIds.contains(chatItem.chatId)) {
            emitter.emit(Frame(chatItem = chatItem))
          }
        }
        count++
      }
    }
  }

  fun beginImport(importState: ImportState): ChatItemArchiveImporter {
    return ZonaRosaDatabase.messages.createChatItemInserter(importState)
  }
}
