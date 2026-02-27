package io.zonarosa.messenger.components.settings.app.privacy

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.util.livedata.Store

class PrivacySettingsViewModel(
  private val sharedPreferences: SharedPreferences,
  private val repository: PrivacySettingsRepository
) : ViewModel() {

  private val store = Store(getState())

  val state: LiveData<PrivacySettingsState> = store.stateLiveData

  fun refreshBlockedCount() {
    repository.getBlockedCount { count ->
      store.update { it.copy(blockedCount = count) }
      refresh()
    }
  }

  fun setReadReceiptsEnabled(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.READ_RECEIPTS_PREF, enabled).apply()
    repository.syncReadReceiptState()
    refresh()
  }

  fun setTypingIndicatorsEnabled(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.TYPING_INDICATORS, enabled).apply()
    repository.syncTypingIndicatorsState()
    refresh()
  }

  fun setScreenSecurityEnabled(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.SCREEN_SECURITY_PREF, enabled).apply()
    refresh()
  }

  fun setIncognitoKeyboard(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.INCOGNITO_KEYBOARD_PREF, enabled).apply()
    refresh()
  }

  fun togglePaymentLock(enable: Boolean) {
    ZonaRosaStore.payments.paymentLock = enable
    refresh()
  }

  fun setObsoletePasswordTimeoutEnabled(enabled: Boolean) {
    ZonaRosaStore.settings.passphraseTimeoutEnabled = enabled
    refresh()
  }

  fun setObsoletePasswordTimeout(minutes: Int) {
    ZonaRosaStore.settings.passphraseTimeout = minutes
    refresh()
  }

  fun refresh() {
    store.update(this::updateState)
  }

  private fun getState(): PrivacySettingsState {
    return PrivacySettingsState(
      blockedCount = 0,
      readReceipts = ZonaRosaPreferences.isReadReceiptsEnabled(AppDependencies.application),
      typingIndicators = ZonaRosaPreferences.isTypingIndicatorsEnabled(AppDependencies.application),
      screenLock = ZonaRosaStore.settings.screenLockEnabled,
      screenLockActivityTimeout = ZonaRosaStore.settings.screenLockTimeout,
      screenSecurity = ZonaRosaPreferences.isScreenSecurityEnabled(AppDependencies.application),
      incognitoKeyboard = ZonaRosaPreferences.isIncognitoKeyboardEnabled(AppDependencies.application),
      paymentLock = ZonaRosaStore.payments.paymentLock,
      isObsoletePasswordEnabled = !ZonaRosaStore.settings.passphraseDisabled,
      isObsoletePasswordTimeoutEnabled = ZonaRosaStore.settings.passphraseTimeoutEnabled,
      obsoletePasswordTimeout = ZonaRosaStore.settings.passphraseTimeout,
      universalExpireTimer = ZonaRosaStore.settings.universalExpireTimer
    )
  }

  private fun updateState(state: PrivacySettingsState): PrivacySettingsState {
    return getState().copy(blockedCount = state.blockedCount)
  }

  class Factory(
    private val sharedPreferences: SharedPreferences,
    private val repository: PrivacySettingsRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(PrivacySettingsViewModel(sharedPreferences, repository)))
    }
  }
}
