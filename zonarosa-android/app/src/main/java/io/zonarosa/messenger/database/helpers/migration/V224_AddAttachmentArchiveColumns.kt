/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds archive_cdn and archive_media to attachment.
 */
@Suppress("ClassName")
object V224_AddAttachmentArchiveColumns : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_cdn INTEGER DEFAULT 0")
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_media_name TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_media_id TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_transfer_file TEXT DEFAULT NULL")
  }
}
