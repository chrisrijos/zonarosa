/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testutil

import java.util.LinkedList
import java.util.Random

class MockRandom(initialInts: List<Int>) : Random() {

  val nextInts = LinkedList(initialInts)

  override fun nextInt(): Int {
    return nextInts.remove()
  }

  override fun nextInt(bound: Int): Int {
    return nextInts.remove() % bound
  }
}
