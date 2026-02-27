/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import io.zonarosa.core.util.forEach
import io.zonarosa.core.util.requireLong
import io.zonarosa.core.util.requireNonNullString
import io.zonarosa.core.util.select
import io.zonarosa.core.util.updateAll
import io.zonarosa.messenger.crash.CrashConfig
import io.zonarosa.messenger.dependencies.AppDependencies

class LogDatabaseTest {

  private val db: LogDatabase = LogDatabase.getInstance(AppDependencies.application)

  @Test
  fun crashTable_matchesNamePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isTrue()
  }

  @Test
  fun crashTable_matchesMessagePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(messagePattern = "Message")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isTrue()
  }

  @Test
  fun crashTable_matchesStackTracePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(stackTracePattern = "stack")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isTrue()
  }

  @Test
  fun crashTable_matchesNameAndMessagePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test", messagePattern = "Message")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isTrue()
  }

  @Test
  fun crashTable_matchesNameAndStackTracePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test", stackTracePattern = "stack")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isTrue()
  }

  @Test
  fun crashTable_matchesNameAndMessageAndStackTracePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test", messagePattern = "Message", stackTracePattern = "stack")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isTrue()
  }

  @Test
  fun crashTable_doesNotMatchNamePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Blah")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isFalse()
  }

  @Test
  fun crashTable_matchesNameButNotMessagePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test", messagePattern = "Blah")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isFalse()
  }

  @Test
  fun crashTable_matchesNameButNotStackTracePattern() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test", stackTracePattern = "Blah")
      ),
      promptThreshold = currentTime
    )

    assertThat(foundMatch).isFalse()
  }

  @Test
  fun crashTable_matchesNamePatternButPromptedTooRecently() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    db.writableDatabase
      .updateAll(LogDatabase.CrashTable.TABLE_NAME)
      .values(LogDatabase.CrashTable.LAST_PROMPTED_AT to currentTime)
      .run()

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test")
      ),
      promptThreshold = currentTime - 100
    )

    assertThat(foundMatch).isFalse()
  }

  @Test
  fun crashTable_noMatches() {
    val currentTime = System.currentTimeMillis()

    val foundMatch = db.crashes.anyMatch(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test")
      ),
      promptThreshold = currentTime - 100
    )

    assertThat(foundMatch).isFalse()
  }

  @Test
  fun crashTable_updatesLastPromptTime() {
    val currentTime = System.currentTimeMillis()

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "TestName",
      message = "Test Message",
      stackTrace = "test\nstack\ntrace"
    )

    db.crashes.saveCrash(
      createdAt = currentTime,
      name = "XXX",
      message = "XXX",
      stackTrace = "XXX"
    )

    db.crashes.markAsPrompted(
      listOf(
        CrashConfig.CrashPattern(namePattern = "Test")
      ),
      promptedAt = currentTime
    )

    db.writableDatabase
      .select(LogDatabase.CrashTable.NAME, LogDatabase.CrashTable.LAST_PROMPTED_AT)
      .from(LogDatabase.CrashTable.TABLE_NAME)
      .run()
      .forEach {
        if (it.requireNonNullString(LogDatabase.CrashTable.NAME) == "TestName") {
          assertThat(it.requireLong(LogDatabase.CrashTable.LAST_PROMPTED_AT)).isEqualTo(currentTime)
        } else {
          assertThat(it.requireLong(LogDatabase.CrashTable.LAST_PROMPTED_AT)).isEqualTo(0)
        }
      }
  }
}
