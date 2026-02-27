/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Updates read status for unread original messages to work with new unread count scheme.
 */
@Suppress("ClassName")
object V273_FixUnreadOriginalMessages : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      """
      UPDATE message 
      SET read = 1 
      WHERE original_message_id IS NULL AND read = 0 AND _id IN (
        SELECT DISTINCT original_message_id 
        FROM message INDEXED BY message_original_message_id_index 
        WHERE read = 1 AND original_message_id NOT NULL
      )"""
    )
  }
}
