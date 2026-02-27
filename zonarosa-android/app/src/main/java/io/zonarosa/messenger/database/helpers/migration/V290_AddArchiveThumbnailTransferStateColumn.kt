/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * We need to keep track of a transfer state for thumbnails too.
 */
@Suppress("ClassName")
object V290_AddArchiveThumbnailTransferStateColumn : ZonaRosaDatabaseMigration {

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE attachment ADD COLUMN archive_thumbnail_transfer_state INTEGER DEFAULT 0;")
  }
}
