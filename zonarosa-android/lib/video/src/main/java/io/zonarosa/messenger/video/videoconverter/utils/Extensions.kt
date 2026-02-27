/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.video.videoconverter.utils

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

object Extensions {
  /**
   * Determines if the [actual] value is close enough to the [expected] value within the [tolerance]
   *
   * @param tolerance a float value, where 0f defines an exact match, 0.1f defines a 10% tolerance, etc.
   */
  @JvmStatic
  fun isWithin(expected: Long, actual: Long, tolerance: Float): Boolean {
    val floor = floor(expected * (1 - tolerance)).roundToLong()
    val ceiling = ceil(expected * (1 + tolerance)).roundToLong()
    return actual in floor..ceiling
  }
}
