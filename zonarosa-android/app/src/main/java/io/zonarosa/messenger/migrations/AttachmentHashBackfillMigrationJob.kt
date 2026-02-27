/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.AttachmentHashBackfillJob
import java.lang.Exception

/**
 * Kicks off the attachment hash backfill process by enqueueing a [AttachmentHashBackfillJob].
 */
internal class AttachmentHashBackfillMigrationJob(parameters: Parameters = Parameters.Builder().build()) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(AttachmentHashBackfillMigrationJob::class.java)
    const val KEY = "AttachmentHashBackfillMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    AppDependencies.jobManager.add(AttachmentHashBackfillJob())
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<AttachmentHashBackfillMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AttachmentHashBackfillMigrationJob {
      return AttachmentHashBackfillMigrationJob(parameters)
    }
  }
}
