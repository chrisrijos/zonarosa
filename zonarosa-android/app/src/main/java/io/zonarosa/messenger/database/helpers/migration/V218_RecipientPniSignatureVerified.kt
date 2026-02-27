/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds a pni_signature_verified column to the recipient table, letting us track whether the ACI/PNI association is verified and sync that to storage service.
 */
@Suppress("ClassName")
object V218_RecipientPniSignatureVerified : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE recipient ADD COLUMN pni_signature_verified INTEGER DEFAULT 0")
  }
}
