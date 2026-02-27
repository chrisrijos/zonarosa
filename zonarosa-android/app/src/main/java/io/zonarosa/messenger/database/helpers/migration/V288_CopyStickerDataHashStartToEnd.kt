/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * Copy data_hash_start to data_hash_end for sticker attachments that have completed transfer.
 */
@Suppress("ClassName")
object V288_CopyStickerDataHashStartToEnd : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(
      "UPDATE attachment SET data_hash_end = data_hash_start WHERE sticker_pack_id IS NOT NULL AND data_hash_start IS NOT NULL AND data_hash_end IS NULL AND transfer_state = 0"
    )
  }
}
