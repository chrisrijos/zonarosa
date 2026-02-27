/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.core.util.readToList
import io.zonarosa.core.util.requireLong
import io.zonarosa.messenger.database.SQLiteDatabase
import java.util.UUID

/**
 * Add notification_profile_id column to Notification Profiles to support backups.
 */
@Suppress("ClassName")
object V271_AddNotificationProfileIdColumn : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE notification_profile ADD COLUMN notification_profile_id TEXT DEFAULT NULL")

    db.rawQuery("SELECT _id FROM notification_profile")
      .readToList { it.requireLong("_id") }
      .forEach { id ->
        db.execSQL("UPDATE notification_profile SET notification_profile_id = '${UUID.randomUUID()}' WHERE _id = $id")
      }
  }
}
