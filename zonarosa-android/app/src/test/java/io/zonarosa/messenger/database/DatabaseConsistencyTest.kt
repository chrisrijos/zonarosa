/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import android.app.Application
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.core.util.ForeignKeyConstraint
import io.zonarosa.core.util.Index
import io.zonarosa.core.util.getForeignKeys
import io.zonarosa.core.util.getIndexes
import io.zonarosa.core.util.readToList
import io.zonarosa.core.util.requireNonNullString
import io.zonarosa.messenger.database.helpers.ZonaRosaDatabaseMigrations
import io.zonarosa.messenger.testutil.ZonaRosaDatabaseMigrationRule
import io.zonarosa.messenger.testutil.ZonaRosaDatabaseRule

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [35])
class DatabaseConsistencyTest {

  @get:Rule
  val zonarosaDatabaseRule = ZonaRosaDatabaseRule()

  @get:Rule
  val zonarosaDatabaseMigrationRule = ZonaRosaDatabaseMigrationRule(ZonaRosaDatabaseMigrations.DATABASE_VERSION)

  @Test
  fun testUpgradeConsistency() {
    val currentVersionStatements = zonarosaDatabaseRule.readableDatabase.getAllCreateStatements()
    val upgradedStatements = zonarosaDatabaseMigrationRule.database.getAllCreateStatements()

    if (currentVersionStatements != upgradedStatements) {
      var message = "\n"

      val currentByName = currentVersionStatements.associateBy { it.name }
      val upgradedByName = upgradedStatements.associateBy { it.name }

      if (currentByName.keys != upgradedByName.keys) {
        val exclusiveToCurrent = currentByName.keys - upgradedByName.keys
        val exclusiveToUpgrade = upgradedByName.keys - currentByName.keys

        message += "SQL entities exclusive to the newly-created database: $exclusiveToCurrent\n"
        message += "SQL entities exclusive to the upgraded database: $exclusiveToUpgrade\n\n"
      } else {
        for (currentEntry in currentByName) {
          val upgradedValue: ZonaRosaDatabaseMigrationRule.Statement = upgradedByName[currentEntry.key]!!
          if (upgradedValue.sql != currentEntry.value.sql) {
            message += "Statement differed:\n"
            message += "newly-created:\n"
            message += "${currentEntry.value.sql}\n\n"
            message += "upgraded:\n"
            message += "${upgradedValue.sql}\n\n"
          }
        }
      }

      Assert.assertTrue(message, false)
    }
  }

  @Test
  fun testForeignKeyIndexCoverage() {
    /** We may deem certain indexes non-critical if deletion frequency is low or table size is small. */
    val ignoredColumns: List<Pair<String, String>> = listOf(
      StorySendTable.TABLE_NAME to StorySendTable.DISTRIBUTION_ID
    )

    val foreignKeys: List<ForeignKeyConstraint> = zonarosaDatabaseRule.writeableDatabase.getForeignKeys()
    val indexesByFirstColumn: List<Index> = zonarosaDatabaseRule.writeableDatabase.getIndexes()

    val notFound: List<Pair<String, String>> = foreignKeys
      .filterNot { ignoredColumns.contains(it.table to it.column) }
      .filterNot { foreignKey ->
        indexesByFirstColumn.hasPrimaryIndexFor(foreignKey.table, foreignKey.column)
      }
      .map { it.table to it.column }

    Assert.assertTrue("Missing indexes to cover: $notFound", notFound.isEmpty())
  }

  private fun List<Index>.hasPrimaryIndexFor(table: String, column: String): Boolean {
    return this.any { index -> index.table == table && index.columns[0] == column }
  }

  private fun SQLiteDatabase.getAllCreateStatements(): List<ZonaRosaDatabaseMigrationRule.Statement> {
    return this
      .rawQuery("SELECT name, sql FROM sqlite_master WHERE sql NOT NULL AND name != 'sqlite_sequence' AND name != 'android_metadata'")
      .readToList { cursor ->
        ZonaRosaDatabaseMigrationRule.Statement(
          name = cursor.requireNonNullString("name"),
          sql = cursor.requireNonNullString("sql").normalizeSql()
        )
      }
      .filterNot { it.name.startsWith("sqlite_stat") }
      .sortedBy { it.name }
  }

  @Suppress("SimplifiableCallChain")
  private fun String.normalizeSql(): String {
    return this
      .split("\n")
      .map { it.trim() }
      .joinToString(separator = " ")
      .replace(Regex.fromLiteral(" ,"), ",")
      .replace(",([^\\s])".toRegex(), ", $1")
      .replace(Regex("\\s+"), " ")
      .replace(Regex.fromLiteral("( "), "(")
      .replace(Regex.fromLiteral(" )"), ")")
      .replace(Regex("CREATE TABLE \"([a-zA-Z_]+)\""), "CREATE TABLE $1") // for some reason SQLite will wrap table names in quotes for upgraded tables. This unwraps them.
  }
}
