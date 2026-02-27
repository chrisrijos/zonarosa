/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

object V231_ArchiveThumbnailColumns : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_thumbnail_cdn INTEGER DEFAULT 0")
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_thumbnail_media_id TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE attachment ADD COLUMN thumbnail_file TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE attachment ADD COLUMN thumbnail_random BLOB DEFAULT NULL")
  }
}
