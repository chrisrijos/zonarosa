/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * We were unnecessarily holding on to some attachment download data for viewed view-once messages that we don't need to hold onto.
 */
object V283_ViewOnceRemoteDataCleanup : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      """
      UPDATE 
        attachment
      SET 
        remote_key = NULL,
        remote_digest = NULL,
        remote_incremental_digest = NULL,
        remote_incremental_digest_chunk_size = 0,
        thumbnail_file = NULL,
        thumbnail_random = NULL,
        archive_transfer_state = 0
      WHERE 
        data_file IS NULL AND
        content_type = 'application/x-zonarosa-view-once'
      """
    )
  }
}
