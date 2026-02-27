/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger

import assertk.Assert
import assertk.assertions.isFalse
import java.util.Optional

fun <T> Assert<Optional<T>>.isAbsent() {
  transform { it.isPresent }.isFalse()
}
