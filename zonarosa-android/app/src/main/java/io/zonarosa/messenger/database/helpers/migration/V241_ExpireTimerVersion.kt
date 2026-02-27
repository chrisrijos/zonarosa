/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Migration for new field tracking expiration timer version.
 */
object V241_ExpireTimerVersion : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE message ADD COLUMN expire_timer_version INTEGER DEFAULT 1 NOT NULL;")
    db.execSQL("ALTER TABLE recipient ADD COLUMN message_expiration_time_version INTEGER DEFAULT 1 NOT NULL;")
    db.execSQL(
      """
      UPDATE recipient
      SET message_expiration_time_version = 2
      WHERE message_expiration_time > 0
      """
    )
  }
}
