/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * In order to make sure the snippet URI is not overwritten by the wrong message attachment, we want to
 * track the snippet message id in the thread table.
 */
@Suppress("ClassName")
object V282_AddSnippetMessageIdColumnToThreadTable : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE thread ADD COLUMN snippet_message_id INTEGER DEFAULT 0")
  }
}
