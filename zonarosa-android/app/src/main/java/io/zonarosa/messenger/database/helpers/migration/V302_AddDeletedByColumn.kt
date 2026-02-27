package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.core.util.Stopwatch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Adds column to messages to track who has deleted a given message
 */
@Suppress("ClassName")
object V302_AddDeletedByColumn : ZonaRosaDatabaseMigration {

  private val TAG = Log.tag(V302_AddDeletedByColumn::class.java)

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    val stopwatch = Stopwatch("migration", decimalPlaces = 2)

    db.execSQL("ALTER TABLE message ADD COLUMN deleted_by INTEGER DEFAULT NULL REFERENCES recipient (_id) ON DELETE CASCADE")
    stopwatch.split("add-column")

    db.execSQL("UPDATE message SET deleted_by = from_recipient_id WHERE remote_deleted > 0")
    stopwatch.split("copy-data")

    db.execSQL("ALTER TABLE message DROP COLUMN remote_deleted")
    stopwatch.split("drop-column")

    db.execSQL("CREATE INDEX message_deleted_by_index ON message (deleted_by)")
    stopwatch.split("create-index")

    stopwatch.stop(TAG)
  }
}
