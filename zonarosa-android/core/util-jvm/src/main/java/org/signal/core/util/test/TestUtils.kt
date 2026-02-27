/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.test

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Returns a string containing the differences between the expected and actual objects.
 * Useful for diffing complex data classes in your tests.
 */
inline fun <reified T : Any> getObjectDiff(expected: T, actual: T): String {
  val builder = StringBuilder()

  val properties = T::class.memberProperties

  for (prop in properties) {
    prop.isAccessible = true
    val expectedValue = prop.get(expected)
    val actualValue = prop.get(actual)
    if (expectedValue != actualValue) {
      builder.append("[${prop.name}] Expected: $expectedValue, Actual: $actualValue\n")
    }
  }

  return builder.toString()
}
