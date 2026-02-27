/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.migrations

import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Schedules job to download both the localized and English emoji search indices, ensuring that emoji search data is available in the user's preferred
 * language as well as English.
 */
internal class EmojiSearchEnglishLabelsMigrationJob(parameters: Parameters = Parameters.Builder().build()) : MigrationJob(parameters) {
  companion object {
    const val KEY = "EmojiSearchEnglishLabelsMigrationJob"
  }

  override fun getFactoryKey(): String = KEY
  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (EmojiSearchIndexDownloadJob.LANGUAGE_CODE_ENGLISH != ZonaRosaStore.emoji.searchLanguage) {
      ZonaRosaStore.emoji.clearSearchIndexMetadata()
      EmojiSearchIndexDownloadJob.scheduleImmediately()
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<EmojiSearchEnglishLabelsMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): EmojiSearchEnglishLabelsMigrationJob {
      return EmojiSearchEnglishLabelsMigrationJob(parameters)
    }
  }
}
