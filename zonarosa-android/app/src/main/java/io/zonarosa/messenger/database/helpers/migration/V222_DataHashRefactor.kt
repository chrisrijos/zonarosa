/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds the new data hash columns and indexes.
 */
@Suppress("ClassName")
object V222_DataHashRefactor : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP INDEX attachment_data_hash_index")
    db.execSQL("ALTER TABLE attachment DROP COLUMN data_hash")

    db.execSQL("ALTER TABLE attachment ADD COLUMN data_hash_start TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE attachment ADD COLUMN data_hash_end TEXT DEFAULT NULL")
    db.execSQL("CREATE INDEX attachment_data_hash_start_index ON attachment (data_hash_start)")
    db.execSQL("CREATE INDEX attachment_data_hash_end_index ON attachment (data_hash_end)")
  }
}
