/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.processor

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.ImportSkips
import io.zonarosa.messenger.backup.v2.ImportState
import io.zonarosa.messenger.backup.v2.database.getThreadsForBackup
import io.zonarosa.messenger.backup.v2.importer.ChatArchiveImporter
import io.zonarosa.messenger.backup.v2.proto.Chat
import io.zonarosa.messenger.backup.v2.proto.Frame
import io.zonarosa.messenger.backup.v2.stream.BackupFrameEmitter
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.RecipientId

/**
 * Handles importing/exporting [Chat] frames for an archive.
 */
object ChatArchiveProcessor {
  val TAG = Log.tag(ChatArchiveProcessor::class.java)

  fun export(db: ZonaRosaDatabase, exportState: ExportState, emitter: BackupFrameEmitter) {
    db.threadTable.getThreadsForBackup(db, exportState, includeImageWallpapers = true).use { reader ->
      for (chat in reader) {
        if (exportState.recipientIds.contains(chat.recipientId)) {
          exportState.threadIds.add(chat.id)
          exportState.threadIdToRecipientId[chat.id] = chat.recipientId
          emitter.emit(Frame(chat = chat))
        } else {
          Log.w(TAG, "dropping thread for deleted recipient ${chat.recipientId}")
        }
      }
    }
  }

  fun import(chat: Chat, importState: ImportState) {
    val recipientId: RecipientId? = importState.remoteToLocalRecipientId[chat.recipientId]
    if (recipientId == null) {
      Log.w(TAG, ImportSkips.missingChatRecipient(chat.id))
      return
    }

    val threadId = ChatArchiveImporter.import(chat, recipientId, importState)
    if (threadId == null) {
      Log.w(TAG, ImportSkips.failedToCreateChat())
      return
    }

    importState.chatIdToLocalRecipientId[chat.id] = recipientId
    importState.chatIdToLocalThreadId[chat.id] = threadId
    importState.chatIdToBackupRecipientId[chat.id] = chat.recipientId
    importState.recipientIdToLocalThreadId[recipientId] = threadId
  }
}
