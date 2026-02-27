package io.zonarosa.messenger.components.settings.conversation.sounds.custom

import android.net.Uri
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.recipients.Recipient

data class CustomNotificationsSettingsState(
  val isInitialLoadComplete: Boolean = false,
  val recipient: Recipient? = null,
  val hasCustomNotifications: Boolean = false,
  val controlsEnabled: Boolean = false,
  val messageVibrateState: RecipientTable.VibrateState = RecipientTable.VibrateState.DEFAULT,
  val messageVibrateEnabled: Boolean = false,
  val messageSound: Uri? = null,
  val callVibrateState: RecipientTable.VibrateState = RecipientTable.VibrateState.DEFAULT,
  val callSound: Uri? = null,
  val showCallingOptions: Boolean = false
)
