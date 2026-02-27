/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * This updates the PNI column to have the proper serialized format.
 */
@Suppress("ClassName")
object V200_ResetPniColumn : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE recipient SET pni = 'PNI:' || pni WHERE pni NOT NULL")
    db.execSQL("ALTER TABLE recipient RENAME COLUMN uuid to aci")
  }
}
