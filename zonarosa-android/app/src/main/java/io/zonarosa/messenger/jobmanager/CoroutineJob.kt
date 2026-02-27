/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobmanager

import kotlinx.coroutines.runBlocking

/**
 * Perform a job utilizing operations that require coroutines. By default,
 * doRun is executed on the Default dispatcher.
 */
abstract class CoroutineJob(parameters: Parameters) : Job(parameters) {

  override fun run(): Result {
    return runBlocking {
      doRun()
    }
  }

  abstract suspend fun doRun(): Result
}
