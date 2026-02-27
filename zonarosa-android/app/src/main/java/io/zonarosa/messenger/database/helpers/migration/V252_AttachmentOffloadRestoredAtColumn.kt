/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.core.util.SqlUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds the offload_restored_at column to attachments.
 *
 * Important: May be ran twice depending on people's upgrade path during the beta.
 */
@Suppress("ClassName")
object V252_AttachmentOffloadRestoredAtColumn : ZonaRosaDatabaseMigration {

  private val TAG = Log.tag(V252_AttachmentOffloadRestoredAtColumn::class)

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (SqlUtil.columnExists(db, "attachment", "offload_restored_at")) {
      Log.i(TAG, "Already ran migration!")
      return
    }

    db.execSQL("ALTER TABLE attachment ADD COLUMN offload_restored_at INTEGER DEFAULT 0;")
  }
}
