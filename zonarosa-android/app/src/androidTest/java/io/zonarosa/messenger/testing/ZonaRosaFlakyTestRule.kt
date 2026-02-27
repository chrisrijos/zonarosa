/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testing

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import io.zonarosa.core.util.logging.Log

/**
 * A JUnit rule that retries tests annotated with [ZonaRosaFlakyTest] before considering them to be a failure.
 * As the name implies, this is useful for known-flaky tests.
 */
class ZonaRosaFlakyTestRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement {
    val flakyAnnotation = description.getAnnotation(ZonaRosaFlakyTest::class.java)

    return if (flakyAnnotation != null) {
      FlakyStatement(
        base = base,
        description = description,
        allowedAttempts = flakyAnnotation.allowedAttempts
      )
    } else {
      base
    }
  }

  private class FlakyStatement(private val base: Statement, private val description: Description, private val allowedAttempts: Int) : Statement() {
    override fun evaluate() {
      var attemptsRemaining = allowedAttempts
      while (attemptsRemaining > 0) {
        try {
          base.evaluate()
          return
        } catch (t: Throwable) {
          attemptsRemaining--
          if (attemptsRemaining <= 0) {
            throw t
          }
          Log.w(description.testClass.simpleName, "[${description.methodName}] Flaky test failed! $attemptsRemaining attempt(s) remaining.", t)
        }
      }
    }
  }
}
