package io.zonarosa.messenger.components.settings.app.notifications

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.notifications.DeviceSpecificNotificationConfig
import io.zonarosa.messenger.notifications.NotificationChannels
import io.zonarosa.messenger.notifications.SlowNotificationHeuristics
import io.zonarosa.messenger.preferences.widgets.NotificationPrivacyPreference
import io.zonarosa.messenger.util.ZonaRosaPreferences

class NotificationsSettingsViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

  private val store = MutableStateFlow(getState())

  val state: StateFlow<NotificationsSettingsState> = store

  init {
    if (NotificationChannels.supported()) {
      ZonaRosaStore.settings.messageNotificationSound = NotificationChannels.getInstance().messageRingtone
      ZonaRosaStore.settings.isMessageVibrateEnabled = NotificationChannels.getInstance().messageVibrate
    }

    // Calculating slow notification stuff isn't thread-safe, so we do it without to start off so we have most state populated, then fetch it in the background.
    store.update { getState(calculateSlowNotifications = false) }
    viewModelScope.launch(Dispatchers.IO) {
      store.update { getState(calculateSlowNotifications = true) }
    }
  }

  fun refresh() {
    store.update { getState(currentState = it) }
  }

  fun setMessageNotificationsEnabled(enabled: Boolean) {
    ZonaRosaStore.settings.isMessageNotificationsEnabled = enabled
    refresh()
  }

  fun setMessageNotificationsSound(sound: Uri?) {
    val messageSound = sound ?: Uri.EMPTY
    ZonaRosaStore.settings.messageNotificationSound = messageSound
    NotificationChannels.getInstance().updateMessageRingtone(messageSound)
    refresh()
  }

  fun setMessageNotificationVibration(enabled: Boolean) {
    ZonaRosaStore.settings.isMessageVibrateEnabled = enabled
    NotificationChannels.getInstance().updateMessageVibrate(enabled)
    refresh()
  }

  fun setMessageNotificationLedColor(color: String) {
    ZonaRosaStore.settings.messageLedColor = color
    NotificationChannels.getInstance().updateMessagesLedColor(color)
    refresh()
  }

  fun setMessageNotificationLedBlink(blink: String) {
    ZonaRosaStore.settings.messageLedBlinkPattern = blink
    refresh()
  }

  fun setMessageNotificationInChatSoundsEnabled(enabled: Boolean) {
    ZonaRosaStore.settings.isMessageNotificationsInChatSoundsEnabled = enabled
    refresh()
  }

  fun setMessageRepeatAlerts(repeats: Int) {
    ZonaRosaStore.settings.messageNotificationsRepeatAlerts = repeats
    refresh()
  }

  fun setMessageNotificationPrivacy(preference: String) {
    ZonaRosaStore.settings.messageNotificationsPrivacy = NotificationPrivacyPreference(preference)
    refresh()
  }

  fun setMessageNotificationPriority(priority: Int) {
    sharedPreferences.edit().putString(ZonaRosaPreferences.NOTIFICATION_PRIORITY_PREF, priority.toString()).apply()
    refresh()
  }

  fun setCallNotificationsEnabled(enabled: Boolean) {
    ZonaRosaStore.settings.isCallNotificationsEnabled = enabled
    refresh()
  }

  fun setCallRingtone(ringtone: Uri?) {
    ZonaRosaStore.settings.callRingtone = ringtone ?: Uri.EMPTY
    refresh()
  }

  fun setCallVibrateEnabled(enabled: Boolean) {
    ZonaRosaStore.settings.isCallVibrateEnabled = enabled
    refresh()
  }

  fun setNotifyWhenContactJoinsZonaRosa(enabled: Boolean) {
    ZonaRosaStore.settings.isNotifyWhenContactJoinsZonaRosa = enabled
    refresh()
  }

  /**
   * @param currentState If provided and [calculateSlowNotifications] = false, then we will copy the slow notification state from it
   * @param calculateSlowNotifications If true, calculate the true slow notification state (this is not main-thread safe). Otherwise, it will copy from
   * [currentState] or default to false.
   */
  private fun getState(currentState: NotificationsSettingsState? = null, calculateSlowNotifications: Boolean = false): NotificationsSettingsState = NotificationsSettingsState(
    messageNotificationsState = MessageNotificationsState(
      notificationsEnabled = ZonaRosaStore.settings.isMessageNotificationsEnabled && canEnableNotifications(),
      canEnableNotifications = canEnableNotifications(),
      sound = ZonaRosaStore.settings.messageNotificationSound,
      vibrateEnabled = ZonaRosaStore.settings.isMessageVibrateEnabled,
      ledColor = ZonaRosaStore.settings.messageLedColor,
      ledBlink = ZonaRosaStore.settings.messageLedBlinkPattern,
      inChatSoundsEnabled = ZonaRosaStore.settings.isMessageNotificationsInChatSoundsEnabled,
      repeatAlerts = ZonaRosaStore.settings.messageNotificationsRepeatAlerts,
      messagePrivacy = ZonaRosaStore.settings.messageNotificationsPrivacy.toString(),
      priority = ZonaRosaPreferences.getNotificationPriority(AppDependencies.application),
      troubleshootNotifications = if (calculateSlowNotifications) {
        (SlowNotificationHeuristics.isBatteryOptimizationsOn() && SlowNotificationHeuristics.isHavingDelayedNotifications()) ||
          SlowNotificationHeuristics.getDeviceSpecificShowCondition() == DeviceSpecificNotificationConfig.ShowCondition.ALWAYS
      } else if (currentState != null) {
        currentState.messageNotificationsState.troubleshootNotifications
      } else {
        false
      }
    ),
    callNotificationsState = CallNotificationsState(
      notificationsEnabled = ZonaRosaStore.settings.isCallNotificationsEnabled && canEnableNotifications(),
      canEnableNotifications = canEnableNotifications(),
      ringtone = ZonaRosaStore.settings.callRingtone,
      vibrateEnabled = ZonaRosaStore.settings.isCallVibrateEnabled
    ),
    notifyWhenContactJoinsZonaRosa = ZonaRosaStore.settings.isNotifyWhenContactJoinsZonaRosa
  )

  private fun canEnableNotifications(): Boolean {
    val areNotificationsDisabledBySystem = Build.VERSION.SDK_INT >= 26 &&
      (
        !NotificationChannels.getInstance().isMessageChannelEnabled ||
          !NotificationChannels.getInstance().isMessagesChannelGroupEnabled ||
          !NotificationChannels.getInstance().areNotificationsEnabled()
        )

    return !areNotificationsDisabledBySystem
  }

  class Factory(private val sharedPreferences: SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(NotificationsSettingsViewModel(sharedPreferences)))
    }
  }
}
