/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase
import io.zonarosa.messenger.database.helpers.migration.V240_MessageFullTextSearchSecureDelete.FTS_TABLE_NAME

/**
 * This undoes [V240_MessageFullTextSearchSecureDelete] by disabling secure-delete on our FTS table.
 * Unfortunately the performance overhead was too high. Thankfully, our old approach, while more
 * manual, provides the same safety guarantees, while also allowing us to optimize bulk deletes.
 */
object V243_MessageFullTextSearchDisableSecureDelete : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("""INSERT INTO $FTS_TABLE_NAME ($FTS_TABLE_NAME, rank) VALUES('secure-delete', 0);""")
  }
}
