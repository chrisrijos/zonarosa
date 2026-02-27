/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.migrations

import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.ProfileUploadJob
import io.zonarosa.messenger.jobs.RefreshAttributesJob
import java.lang.Exception

/**
 * Kicks off a chain of jobs to update the server with our latest PNP settings.
 */
internal class PnpLaunchMigrationJob(parameters: Parameters = Parameters.Builder().build()) : MigrationJob(parameters) {
  companion object {
    const val KEY = "PnpLaunchMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    AppDependencies.jobManager
      .startChain(RefreshAttributesJob())
      .then(ProfileUploadJob())
      .enqueue()
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<PnpLaunchMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): PnpLaunchMigrationJob {
      return PnpLaunchMigrationJob(parameters)
    }
  }
}
