/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Removes the UNIQUE constraint from kyber_prekey_id field
 */
@Suppress("ClassName")
object V294_RemoveLastResortKeyTupleColumnConstraintMigration : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("DROP TABLE IF EXISTS last_resort_key_tuple")

    db.execSQL(
      """
      CREATE TABLE last_resort_key_tuple (
        _id INTEGER PRIMARY KEY,
        kyber_prekey_id INTEGER NOT NULL REFERENCES kyber_prekey (_id) ON DELETE CASCADE,
        signed_key_id INTEGER NOT NULL,
        public_key BLOB NOT NULL,
        UNIQUE(kyber_prekey_id, signed_key_id, public_key)
      )
      """
    )
  }
}
