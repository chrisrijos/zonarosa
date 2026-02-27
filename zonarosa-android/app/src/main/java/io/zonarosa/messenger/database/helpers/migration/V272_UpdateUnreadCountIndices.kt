/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Updates a partial index for some performance-critical queries around unread counts.
 */
@Suppress("ClassName")
object V272_UpdateUnreadCountIndices : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP INDEX message_thread_unread_count_index")

    db.execSQL("CREATE INDEX IF NOT EXISTS message_thread_unread_count_index ON message (thread_id) WHERE story_type = 0 AND parent_story_id <= 0 AND scheduled_date = -1 AND original_message_id IS NULL AND read = 0")
  }
}
