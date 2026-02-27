/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Thumbnails are best effort and assumed to have the same CDN as the full attachment, there is no need to store it in the database.
 */
@Suppress("ClassName")
object V246_DropThumbnailCdnFromAttachments : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE attachment DROP COLUMN archive_thumbnail_cdn")
  }
}
