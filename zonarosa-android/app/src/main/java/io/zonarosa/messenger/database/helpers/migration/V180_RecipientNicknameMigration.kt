package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds support for storing the systemNickname from storage service.
 */
@Suppress("ClassName")
object V180_RecipientNicknameMigration : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE recipient ADD COLUMN system_nickname TEXT DEFAULT NULL")
  }
}
