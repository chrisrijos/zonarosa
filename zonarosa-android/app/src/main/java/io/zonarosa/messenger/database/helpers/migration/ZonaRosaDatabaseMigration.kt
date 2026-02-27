package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Simple interface for allowing database migrations to live outside of [io.zonarosa.messenger.database.helpers.ZonaRosaDatabaseMigrations].
 */
interface ZonaRosaDatabaseMigration {
  /** True if you want foreign key constraints to be enforced during a migration, otherwise false. Defaults to false. */
  val enableForeignKeys: Boolean
    get() = false

  fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
}
