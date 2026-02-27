package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * This migration used to do what [V191_UniqueMessageMigrationV2] does. However, due to bugs, the migration was abandoned.
 * We now re-do the migration in V191.
 */
@Suppress("ClassName")
object V190_UniqueMessageMigration : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
