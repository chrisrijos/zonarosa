/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 */
@Suppress("ClassName")
object V211_ReceiptColumnRenames : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("ALTER TABLE message RENAME COLUMN delivery_receipt_count TO has_delivery_receipt")
    db.execSQL("ALTER TABLE message RENAME COLUMN read_receipt_count TO has_read_receipt")
    db.execSQL("ALTER TABLE message RENAME COLUMN viewed_receipt_count TO viewed")

    db.execSQL("ALTER TABLE thread RENAME COLUMN delivery_receipt_count TO has_delivery_receipt")
    db.execSQL("ALTER TABLE thread RENAME COLUMN read_receipt_count TO has_read_receipt")
  }
}
