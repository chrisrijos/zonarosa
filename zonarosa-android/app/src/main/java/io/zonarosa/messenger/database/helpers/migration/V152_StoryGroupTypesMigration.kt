package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Marks story recipients with a new group type constant.
 */
@Suppress("ClassName")
object V152_StoryGroupTypesMigration : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      """
        UPDATE recipient
        SET group_type = 4
        WHERE distribution_list_id IS NOT NULL
      """.trimIndent()
    )
  }
}
