/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Ensure archive_transfer_state is clear if an attachment is missing a remote_key.
 */
@Suppress("ClassName")
object V287_FixInvalidArchiveState : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE attachment SET archive_cdn = null, archive_transfer_state = 0 WHERE remote_key IS NULL AND archive_transfer_state = 3")
  }
}
