/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Reset last forced update timestamp for groups to fix a local group state bug.
 */
@Suppress("ClassName")
object V237_ResetGroupForceUpdateTimestamps : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE groups SET last_force_update_timestamp = 0")
  }
}
