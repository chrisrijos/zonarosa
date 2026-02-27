/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.helpers.migration

import android.app.Application
import io.zonarosa.messenger.database.SQLiteDatabase

/**
 * PNIs were incorrectly being set to ACI column, clear them if present.
 */
@Suppress("ClassName")
object V209_ClearRecipientPniFromAciColumn : ZonaRosaDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL("UPDATE recipient SET aci = NULL WHERE aci LIKE 'PNI:%'")
  }
}
