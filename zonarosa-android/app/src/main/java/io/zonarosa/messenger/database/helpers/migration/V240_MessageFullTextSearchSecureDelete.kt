/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Sets the 'secure-delete' flag on the message_fts table.
 * https://www.sqlite.org/fts5.html#the_secure_delete_configuration_option
 */
@Suppress("ClassName")
object V240_MessageFullTextSearchSecureDelete : ZonaRosaDatabaseMigration {

  const val FTS_TABLE_NAME = "message_fts"

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("""INSERT INTO $FTS_TABLE_NAME ($FTS_TABLE_NAME, rank) VALUES('secure-delete', 1);""")
  }
}
