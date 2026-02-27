/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * If remote_key is an empty byte array (base64 encoded), replace with null.
 */
@Suppress("ClassName")
object V291_NullOutRemoteKeyIfEmpty : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE attachment SET remote_key = NULL WHERE remote_key IS NOT NULL AND LENGTH(remote_key) = 0")
  }
}
