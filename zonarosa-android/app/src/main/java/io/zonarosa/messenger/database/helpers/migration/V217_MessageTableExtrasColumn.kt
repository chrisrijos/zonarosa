/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds a message_extras column to the messages table. This allows us to
 * store extra data for messages in a more future proof and structured way.
 */
@Suppress("ClassName")
object V217_MessageTableExtrasColumn : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE message ADD COLUMN message_extras BLOB DEFAULT NULL")
    db.execSQL("ALTER TABLE thread ADD COLUMN snippet_message_extras BLOB DEFAULT NULL")
  }
}
