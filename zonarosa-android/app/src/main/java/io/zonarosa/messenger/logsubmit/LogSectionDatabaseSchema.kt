package io.zonarosa.messenger.logsubmit

import android.content.Context
import io.zonarosa.core.util.getAllIndexDefinitions
import io.zonarosa.core.util.getAllTableDefinitions
import io.zonarosa.core.util.getAllTriggerDefinitions
import io.zonarosa.core.util.getForeignKeys
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.helpers.ZonaRosaDatabaseMigrations

/**
 * Renders data pertaining to sender key. While all private info is obfuscated, this is still only intended to be printed for internal users.
 */
class LogSectionDatabaseSchema : LogSection {
  override fun getTitle(): String {
    return "DATABASE SCHEMA"
  }

  override fun getContent(context: Context): CharSequence {
    val builder = StringBuilder()
    builder.append("--- Metadata").append("\n")
    builder.append("Version: ${ZonaRosaDatabaseMigrations.DATABASE_VERSION}\n")
    builder.append("\n\n")

    builder.append("--- Tables").append("\n")
    ZonaRosaDatabase.rawDatabase.getAllTableDefinitions().forEach {
      builder.append(it.statement).append("\n")
    }
    builder.append("\n\n")

    builder.append("--- Indexes").append("\n")
    ZonaRosaDatabase.rawDatabase.getAllIndexDefinitions().forEach {
      builder.append(it.statement).append("\n")
    }
    builder.append("\n\n")

    builder.append("--- Foreign Keys").append("\n")
    ZonaRosaDatabase.rawDatabase.getForeignKeys().forEach {
      builder.append("${it.table}.${it.column} DEPENDS ON ${it.dependsOnTable}.${it.dependsOnColumn}, ON DELETE ${it.onDelete}").append("\n")
    }
    builder.append("\n\n")

    builder.append("--- Triggers").append("\n")
    ZonaRosaDatabase.rawDatabase.getAllTriggerDefinitions().forEach {
      builder.append(it.statement).append("\n")
    }
    builder.append("\n\n")

    return builder
  }
}
