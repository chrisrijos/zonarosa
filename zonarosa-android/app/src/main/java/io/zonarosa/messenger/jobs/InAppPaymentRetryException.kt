/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

/**
 * Denotes that a thrown exception can be retried
 */
class InAppPaymentRetryException(
  cause: Throwable? = null
) : Exception(cause)
