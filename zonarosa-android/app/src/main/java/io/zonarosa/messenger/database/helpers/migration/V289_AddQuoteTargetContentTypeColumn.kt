/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds the quote_target_content_type column to attachments and migrates existing quote attachments
 * to populate this field with their current content_type.
 */
@Suppress("ClassName")
object V289_AddQuoteTargetContentTypeColumn : ZonaRosaDatabaseMigration {

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE attachment ADD COLUMN quote_target_content_type TEXT DEFAULT NULL;")
    db.execSQL("UPDATE attachment SET quote_target_content_type = content_type WHERE quote != 0;")
  }
}
