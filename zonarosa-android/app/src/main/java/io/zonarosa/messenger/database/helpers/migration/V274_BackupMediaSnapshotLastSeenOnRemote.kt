/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Added a column to the backup media snapshot table to keep track of the last time we saw an object on the CDN.
 */
object V274_BackupMediaSnapshotLastSeenOnRemote : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE backup_media_snapshot ADD COLUMN last_seen_on_remote_timestamp INTEGER DEFAULT 0")
  }
}
