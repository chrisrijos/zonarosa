/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.core.util.Base64
import io.zonarosa.messenger.database.SQLiteDatabase
import io.zonarosa.messenger.storage.StorageSyncHelper
import java.util.UUID

/**
 * Ensures that there is a default 'All chat' within chat folders.
 */
object V275_EnsureDefaultAllChatsFolder : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      """
      INSERT INTO chat_folder(position, folder_type, show_individual, show_groups, show_muted, chat_folder_id, storage_service_id)
      SELECT '0', '0', '1', '1', '1', '${UUID.randomUUID()}', '${Base64.encodeWithPadding(StorageSyncHelper.generateKey())}'
      WHERE NOT EXISTS (
        SELECT 1
          FROM chat_folder
          WHERE folder_type = 0
          LIMIT 1
      );
      """.trimIndent()
    )
  }
}
