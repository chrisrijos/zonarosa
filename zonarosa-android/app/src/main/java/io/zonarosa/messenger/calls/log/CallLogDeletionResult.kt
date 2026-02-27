/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.log

sealed interface CallLogDeletionResult {
  object Success : CallLogDeletionResult

  object Empty : CallLogDeletionResult
  data class FailedToRevoke(val failedRevocations: Int) : CallLogDeletionResult
  data class UnknownFailure(val reason: Throwable) : CallLogDeletionResult
}
