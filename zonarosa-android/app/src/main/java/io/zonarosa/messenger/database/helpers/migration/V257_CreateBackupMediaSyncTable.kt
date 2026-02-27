/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

@Suppress("ClassName")
object V257_CreateBackupMediaSyncTable : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      """
      CREATE TABLE backup_media_snapshot (
          _id INTEGER PRIMARY KEY,
          media_id TEXT UNIQUE,
          cdn INTEGER,
          last_sync_time INTEGER DEFAULT 0,
          pending_sync_time INTEGER
      )
      """.trimIndent()
    )
  }
}
