package io.zonarosa.messenger.components.settings.app.data

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.mms.SentMediaQuality
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.webrtc.CallDataMode

class DataAndStorageSettingsViewModel(
  private val sharedPreferences: SharedPreferences,
  private val repository: DataAndStorageSettingsRepository
) : ViewModel() {

  private val store = MutableStateFlow(getState())

  val state: StateFlow<DataAndStorageSettingsState> = store

  fun refresh() {
    repository.getTotalStorageUse { totalStorageUse ->
      store.update { getState().copy(totalStorageUse = totalStorageUse) }
    }
  }

  fun setMobileAutoDownloadValues(resultSet: Set<String>) {
    sharedPreferences.edit().putStringSet(ZonaRosaPreferences.MEDIA_DOWNLOAD_MOBILE_PREF, resultSet).apply()
    getStateAndCopyStorageUsage()
  }

  fun setWifiAutoDownloadValues(resultSet: Set<String>) {
    sharedPreferences.edit().putStringSet(ZonaRosaPreferences.MEDIA_DOWNLOAD_WIFI_PREF, resultSet).apply()
    getStateAndCopyStorageUsage()
  }

  fun setRoamingAutoDownloadValues(resultSet: Set<String>) {
    sharedPreferences.edit().putStringSet(ZonaRosaPreferences.MEDIA_DOWNLOAD_ROAMING_PREF, resultSet).apply()
    getStateAndCopyStorageUsage()
  }

  fun setCallDataMode(callDataMode: CallDataMode) {
    ZonaRosaStore.settings.callDataMode = callDataMode
    AppDependencies.zonarosaCallManager.dataModeUpdate()
    getStateAndCopyStorageUsage()
  }

  fun setSentMediaQuality(sentMediaQuality: SentMediaQuality) {
    ZonaRosaStore.settings.sentMediaQuality = sentMediaQuality
    getStateAndCopyStorageUsage()
  }

  private fun getStateAndCopyStorageUsage() {
    store.update { getState().copy(totalStorageUse = it.totalStorageUse) }
  }

  private fun getState() = DataAndStorageSettingsState(
    totalStorageUse = 0,
    mobileAutoDownloadValues = ZonaRosaPreferences.getMobileMediaDownloadAllowed(
      AppDependencies.application
    ),
    wifiAutoDownloadValues = ZonaRosaPreferences.getWifiMediaDownloadAllowed(
      AppDependencies.application
    ),
    roamingAutoDownloadValues = ZonaRosaPreferences.getRoamingMediaDownloadAllowed(
      AppDependencies.application
    ),
    callDataMode = ZonaRosaStore.settings.callDataMode,
    isProxyEnabled = ZonaRosaStore.proxy.isProxyEnabled,
    sentMediaQuality = ZonaRosaStore.settings.sentMediaQuality
  )

  class Factory(
    private val sharedPreferences: SharedPreferences,
    private val repository: DataAndStorageSettingsRepository
  ) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(DataAndStorageSettingsViewModel(sharedPreferences, repository)))
    }
  }
}
