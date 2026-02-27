package io.zonarosa.messenger.components.settings.app.data

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies

class DataAndStorageSettingsRepository {

  private val context: Context = AppDependencies.application

  fun getTotalStorageUse(consumer: (Long) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val breakdown = ZonaRosaDatabase.media.getStorageBreakdown()

      consumer(listOf(breakdown.audioSize, breakdown.documentSize, breakdown.photoSize, breakdown.videoSize).sum())
    }
  }
}
