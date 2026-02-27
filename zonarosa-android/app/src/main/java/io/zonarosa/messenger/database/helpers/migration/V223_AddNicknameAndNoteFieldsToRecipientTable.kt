/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds necessary fields to the recipeints table for the nickname & notes feature.
 */
@Suppress("ClassName")
object V223_AddNicknameAndNoteFieldsToRecipientTable : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE recipient ADD COLUMN nickname_given_name TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE recipient ADD COLUMN nickname_family_name TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE recipient ADD COLUMN nickname_joined_name TEXT DEFAULT NULL")
    db.execSQL("ALTER TABLE recipient ADD COLUMN note TEXT DEFAULT NULL")
  }
}
