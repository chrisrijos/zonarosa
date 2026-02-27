/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.migrations

import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.update
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.AttachmentTable.Companion.DATA_FILE
import io.zonarosa.messenger.database.AttachmentTable.Companion.QUOTE
import io.zonarosa.messenger.database.AttachmentTable.Companion.QUOTE_PENDING_TRANSCODE
import io.zonarosa.messenger.database.AttachmentTable.Companion.TABLE_NAME
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.QuoteThumbnailBackfillJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import java.lang.Exception

/**
 * Kicks off the quote attachment thumbnail generation process by marking quote attachments
 * for processing and enqueueing a [QuoteThumbnailBackfillJob].
 */
internal class QuoteThumbnailBackfillMigrationJob(parameters: Parameters = Parameters.Builder().build()) : MigrationJob(parameters) {

  companion object {
    val TAG = Log.tag(QuoteThumbnailBackfillMigrationJob::class.java)
    const val KEY = "QuoteThumbnailBackfillMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    val markedCount = ZonaRosaDatabase.attachments.migrationMarkQuoteAttachmentsForThumbnailProcessing()
    ZonaRosaStore.misc.startedQuoteThumbnailMigration = true

    Log.i(TAG, "Marked $markedCount quote attachments for thumbnail processing")

    if (markedCount > 0) {
      AppDependencies.jobManager.add(QuoteThumbnailBackfillJob())
    } else {
      Log.i(TAG, "No quote attachments to process.")
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  private fun AttachmentTable.migrationMarkQuoteAttachmentsForThumbnailProcessing(): Int {
    return writableDatabase
      .update(TABLE_NAME)
      .values(QUOTE to QUOTE_PENDING_TRANSCODE)
      .where("$QUOTE != 0 AND $DATA_FILE NOT NULL")
      .run()
  }

  class Factory : Job.Factory<QuoteThumbnailBackfillMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): QuoteThumbnailBackfillMigrationJob {
      return QuoteThumbnailBackfillMigrationJob(parameters)
    }
  }
}
