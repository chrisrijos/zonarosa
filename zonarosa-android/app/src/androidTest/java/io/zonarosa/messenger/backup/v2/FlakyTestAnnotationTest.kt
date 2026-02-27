/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.messenger.testing.ZonaRosaFlakyTest
import io.zonarosa.messenger.testing.ZonaRosaFlakyTestRule

@RunWith(AndroidJUnit4::class)
class FlakyTestAnnotationTest {

  @get:Rule
  val flakyTestRule = ZonaRosaFlakyTestRule()

  companion object {
    private var count = 0
  }

  @ZonaRosaFlakyTest
  @Test
  fun purposelyFlaky() {
    count++
    assertEquals(3, count)
  }
}
