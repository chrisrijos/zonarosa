/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import java.util.Optional

fun <E> Optional<E>.or(other: Optional<E>): Optional<E> {
  return if (this.isPresent) {
    this
  } else {
    other
  }
}

fun <E> Optional<E>.isAbsent(): Boolean {
  return !isPresent
}

fun <E : Any> E?.toOptional(): Optional<E> {
  return Optional.ofNullable(this)
}

fun <E> Optional<E>.orNull(): E? {
  return orElse(null)
}
