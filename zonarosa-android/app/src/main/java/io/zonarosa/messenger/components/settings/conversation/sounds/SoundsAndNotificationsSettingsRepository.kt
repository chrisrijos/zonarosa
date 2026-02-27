package io.zonarosa.messenger.components.settings.conversation.sounds

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.notifications.NotificationChannels
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

class SoundsAndNotificationsSettingsRepository(private val context: Context) {

  fun ensureCustomChannelConsistency(complete: () -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      if (NotificationChannels.supported()) {
        NotificationChannels.getInstance().ensureCustomChannelConsistency()
      }
      complete()
    }
  }

  fun setMuteUntil(recipientId: RecipientId, muteUntil: Long) {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.setMuted(recipientId, muteUntil)
    }
  }

  fun setMentionSetting(recipientId: RecipientId, mentionSetting: RecipientTable.MentionSetting) {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.setMentionSetting(recipientId, mentionSetting)
    }
  }

  fun hasCustomNotificationSettings(recipientId: RecipientId, consumer: (Boolean) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipient = Recipient.resolved(recipientId)
      consumer(
        if (recipient.notificationChannel != null || !NotificationChannels.supported()) {
          true
        } else {
          NotificationChannels.getInstance().updateWithShortcutBasedChannel(recipient)
        }
      )
    }
  }
}
