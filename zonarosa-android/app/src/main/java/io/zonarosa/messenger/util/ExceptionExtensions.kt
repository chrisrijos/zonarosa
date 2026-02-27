/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

@file:JvmName("ExceptionHelper")

package io.zonarosa.messenger.util

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException
import java.io.IOException

/**
 * Returns true if this exception is a retryable I/O Exception. Helpful for jobs.
 */
fun Throwable.isRetryableIOException(): Boolean {
  return this is IOException && this !is NonSuccessfulResponseCodeException
}
