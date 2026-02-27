/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * In order to both correct how we display missed calls and not spam users,
 * we want to mark every missed call event in the database as notified.
 */
@Suppress("ClassName")
object V229_MarkMissedCallEventsNotified : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      """
      UPDATE message
      SET notified = 1
      WHERE (type = 3) OR (type = 8)
      """.trimIndent()
    )
  }
}
