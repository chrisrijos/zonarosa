/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Add columns to group and group membership tables needed for group send endorsements.
 */
@Suppress("ClassName")
object V238_AddGroupSendEndorsementsColumns : ZonaRosaDatabaseMigration {

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE groups ADD COLUMN group_send_endorsements_expiration INTEGER DEFAULT 0")
    db.execSQL("ALTER TABLE group_membership ADD COLUMN endorsement BLOB DEFAULT NULL")
  }
}
