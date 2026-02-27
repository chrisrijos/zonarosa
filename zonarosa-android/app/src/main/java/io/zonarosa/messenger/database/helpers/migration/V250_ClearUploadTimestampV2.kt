/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Turns out we need to run [V247_ClearUploadTimestamp] again, because there was another situation where we had mismatching transit data across duplicates.
 */
@Suppress("ClassName")
object V250_ClearUploadTimestampV2 : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE attachment SET upload_timestamp = 1 WHERE upload_timestamp > 0")
  }
}
