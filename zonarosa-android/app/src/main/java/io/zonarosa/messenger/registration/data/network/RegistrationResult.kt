/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data.network

import io.zonarosa.core.util.logging.Log

/**
 * This is a merging of the NetworkResult pattern and the Processor pattern of registration v1.
 * The goal is to enumerate all possible responses as sealed classes, which means the consumer will be able to handle them in an exhaustive when clause
 *
 * @property errorCause the [Throwable] that caused the Error. Null if the network request was successful.
 *
 */
abstract class RegistrationResult(private val errorCause: Throwable?) {
  fun isSuccess(): Boolean {
    return errorCause == null
  }

  fun getCause(): Throwable {
    if (errorCause == null) {
      throw IllegalStateException("Cannot get cause from successful processor!")
    }

    return errorCause
  }

  fun logCause() {
    errorCause?.let {
      Log.w(Log.tag(this::class), "Cause:", it)
    }
  }
}
