package io.zonarosa.messenger.components.settings.app.privacy

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.MultiDeviceConfigurationUpdateJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.ZonaRosaPreferences

class PrivacySettingsRepository {

  private val context: Context = AppDependencies.application

  fun getBlockedCount(consumer: (Int) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipientDatabase = ZonaRosaDatabase.recipients

      consumer(recipientDatabase.getBlocked().size)
    }
  }

  fun syncReadReceiptState() {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
      AppDependencies.jobManager.add(
        MultiDeviceConfigurationUpdateJob(
          ZonaRosaPreferences.isReadReceiptsEnabled(context),
          ZonaRosaPreferences.isTypingIndicatorsEnabled(context),
          ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
          ZonaRosaStore.settings.isLinkPreviewsEnabled
        )
      )
    }
  }

  fun syncTypingIndicatorsState() {
    val enabled = ZonaRosaPreferences.isTypingIndicatorsEnabled(context)

    ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()
    AppDependencies.jobManager.add(
      MultiDeviceConfigurationUpdateJob(
        ZonaRosaPreferences.isReadReceiptsEnabled(context),
        enabled,
        ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
        ZonaRosaStore.settings.isLinkPreviewsEnabled
      )
    )

    if (!enabled) {
      AppDependencies.typingStatusRepository.clear()
    }
  }
}
