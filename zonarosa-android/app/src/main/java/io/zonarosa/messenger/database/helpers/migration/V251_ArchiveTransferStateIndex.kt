/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds an index to improve the perf of counting and filtering attachment rows by their transfer state.
 *
 * Important: May be ran twice depending on people's upgrade path during the beta.
 */
@Suppress("ClassName")
object V251_ArchiveTransferStateIndex : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("CREATE INDEX IF NOT EXISTS attachment_archive_transfer_state ON attachment (archive_transfer_state)")
  }
}
