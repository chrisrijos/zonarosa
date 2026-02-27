/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * We planned to introduce CallLink epochs as a first-class field to clients.
 * Now, we plan to introduce epochs as an internal detail in CallLink root keys.
 * Epochs were never enabled in production so no clients should have them.
 */
object V301_RemoveCallLinkEpoch : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE call_link DROP COLUMN epoch")
  }
}
