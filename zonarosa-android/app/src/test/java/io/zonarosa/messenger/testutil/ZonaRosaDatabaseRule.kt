/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testutil

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.ExternalResource
import io.zonarosa.messenger.database.SQLiteDatabase
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.testing.TestZonaRosaDatabase

class ZonaRosaDatabaseRule : ExternalResource() {

  lateinit var zonarosaDatabase: TestZonaRosaDatabase

  val readableDatabase: SQLiteDatabase
    get() = zonarosaDatabase.zonarosaReadableDatabase

  val writeableDatabase: SQLiteDatabase
    get() = zonarosaDatabase.zonarosaWritableDatabase

  override fun before() {
    zonarosaDatabase = inMemoryZonaRosaDatabase()

    mockkObject(ZonaRosaDatabase)
    every { ZonaRosaDatabase.instance } returns zonarosaDatabase
  }

  override fun after() {
    unmockkObject(ZonaRosaDatabase)
    zonarosaDatabase.close()
  }

  companion object {
    /**
     * Create an in-memory only database mimicking one created fresh for ZonaRosa. This includes
     * all non-FTS tables, indexes, and triggers.
     */
    private fun inMemoryZonaRosaDatabase(): TestZonaRosaDatabase {
      val configuration = SupportSQLiteOpenHelper.Configuration(
        context = ApplicationProvider.getApplicationContext(),
        name = "test",
        callback = object : SupportSQLiteOpenHelper.Callback(1) {
          override fun onCreate(db: SupportSQLiteDatabase) = Unit
          override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        },
        useNoBackupDirectory = false,
        allowDataLossOnRecovery = true
      )

      val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
      val zonarosaDatabase = TestZonaRosaDatabase(ApplicationProvider.getApplicationContext(), helper)
      zonarosaDatabase.onCreateTablesIndexesAndTriggers(zonarosaDatabase.zonarosaWritableDatabase)

      return zonarosaDatabase
    }
  }
}
