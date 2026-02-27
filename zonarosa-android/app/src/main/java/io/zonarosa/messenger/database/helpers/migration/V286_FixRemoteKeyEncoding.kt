/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import androidx.core.content.contentValuesOf
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.forEach
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.requireLong
import io.zonarosa.core.util.requireNonNullString
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Ensure remote_key is encoded with padding, fixing archive restore attachments not using padding.
 */
@Suppress("ClassName")
object V286_FixRemoteKeyEncoding : ZonaRosaDatabaseMigration {

  private val TAG = Log.tag(V286_FixRemoteKeyEncoding::class)

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    var updated = 0
    db.query("SELECT _id, remote_key FROM attachment WHERE remote_key is not null AND LENGTH(remote_key) = 86").forEach {
      val id = it.requireLong("_id")
      val remoteKey = Base64.encodeWithPadding(Base64.decode(it.requireNonNullString("remote_key")))

      updated += db.update(
        "attachment",
        contentValuesOf("remote_key" to remoteKey),
        "_id = ? AND remote_key != ?",
        arrayOf(id.toString(), remoteKey)
      )
    }

    Log.i(TAG, "Updated $updated attachment remote_keys")
  }
}
