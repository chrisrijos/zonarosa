package io.zonarosa.messenger.components.settings.app.chats

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.MultiDeviceConfigurationUpdateJob
import io.zonarosa.messenger.jobs.MultiDeviceContactUpdateJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.ZonaRosaPreferences

class ChatsSettingsRepository {

  private val context: Context = AppDependencies.application

  fun syncLinkPreviewsState() {
    ZonaRosaExecutors.BOUNDED.execute {
      val isLinkPreviewsEnabled = ZonaRosaStore.settings.isLinkPreviewsEnabled

      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
      AppDependencies.jobManager.add(
        MultiDeviceConfigurationUpdateJob(
          ZonaRosaPreferences.isReadReceiptsEnabled(context),
          ZonaRosaPreferences.isTypingIndicatorsEnabled(context),
          ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
          isLinkPreviewsEnabled
        )
      )
    }
  }

  fun syncPreferSystemContactPhotos() {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      AppDependencies.jobManager.add(MultiDeviceContactUpdateJob(true))
      StorageSyncHelper.scheduleSyncForDataChange()
    }
  }

  fun syncKeepMutedChatsArchivedState() {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }
  }
}
