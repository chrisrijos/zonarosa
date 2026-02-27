package io.zonarosa.messenger.components.settings.app.privacy.advanced

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.RefreshAttributesJob
import io.zonarosa.messenger.jobs.RefreshOwnProfileJob
import io.zonarosa.messenger.keyvalue.SettingsValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.ZonaRosaE164Util
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.websocket.WebSocketConnectionState

class AdvancedPrivacySettingsViewModel(
  private val sharedPreferences: SharedPreferences,
  private val repository: AdvancedPrivacySettingsRepository
) : ViewModel() {

  private val store = MutableStateFlow(getState())
  private val singleEvents = MutableSharedFlow<Event>()

  val state: StateFlow<AdvancedPrivacySettingsState> = store
  val events: SharedFlow<Event> = singleEvents
  val disposables: CompositeDisposable = CompositeDisposable()

  init {
    disposables.add(
      AppDependencies.webSocketObserver
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { refresh() }
    )
  }

  fun setAlwaysRelayCalls(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.ALWAYS_RELAY_CALLS_PREF, enabled).apply()
    refresh()
  }

  fun setShowStatusIconForSealedSender(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.SHOW_UNIDENTIFIED_DELIVERY_INDICATORS, enabled).apply()
    repository.syncShowSealedSenderIconState()
    refresh()
  }

  fun setAllowSealedSenderFromAnyone(enabled: Boolean) {
    sharedPreferences.edit().putBoolean(ZonaRosaPreferences.UNIVERSAL_UNIDENTIFIED_ACCESS, enabled).apply()
    AppDependencies.jobManager.startChain(RefreshAttributesJob()).then(RefreshOwnProfileJob()).enqueue()
    refresh()
  }

  fun setCensorshipCircumventionEnabled(enabled: Boolean) {
    ZonaRosaStore.settings.setCensorshipCircumventionEnabled(enabled)
    ZonaRosaStore.misc.isServiceReachableWithoutCircumvention = false
    AppDependencies.resetNetwork()
    refresh()
  }

  fun setAllowAutomaticVerification(enabled: Boolean) {
    ZonaRosaStore.settings.automaticVerificationEnabled = enabled
    refresh()
    viewModelScope.launch(ZonaRosaDispatchers.IO) {
      if (!enabled) {
        ZonaRosaDatabase.recipients.clearAllKeyTransparencyData()
      }
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }
  }

  fun refresh() {
    store.update { getState().copy(showProgressSpinner = it.showProgressSpinner) }
  }

  override fun onCleared() {
    disposables.dispose()
  }

  private fun getState(): AdvancedPrivacySettingsState {
    val censorshipCircumventionState = getCensorshipCircumventionState()

    return AdvancedPrivacySettingsState(
      isPushEnabled = ZonaRosaStore.account.isRegistered,
      alwaysRelayCalls = ZonaRosaPreferences.isTurnOnly(AppDependencies.application),
      censorshipCircumventionState = censorshipCircumventionState,
      censorshipCircumventionEnabled = getCensorshipCircumventionEnabled(censorshipCircumventionState),
      showSealedSenderStatusIcon = ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(
        AppDependencies.application
      ),
      allowSealedSenderFromAnyone = ZonaRosaPreferences.isUniversalUnidentifiedAccess(
        AppDependencies.application
      ),
      showProgressSpinner = false,
      allowAutomaticKeyVerification = ZonaRosaStore.settings.automaticVerificationEnabled
    )
  }

  private fun getCensorshipCircumventionState(): CensorshipCircumventionState {
    val countryCode: Int = ZonaRosaE164Util.getLocalCountryCode()
    val isCountryCodeCensoredByDefault: Boolean = AppDependencies.zonarosaServiceNetworkAccess.isCountryCodeCensoredByDefault(countryCode)
    val enabledState: SettingsValues.CensorshipCircumventionEnabled = ZonaRosaStore.settings.censorshipCircumventionEnabled
    val hasInternet: Boolean = NetworkConstraint.isMet(AppDependencies.application)
    val websocketConnected: Boolean = AppDependencies.authWebSocket.state.firstOrError().blockingGet() == WebSocketConnectionState.CONNECTED

    return when {
      ZonaRosaStore.internal.allowChangingCensorshipSetting -> {
        CensorshipCircumventionState.AVAILABLE
      }
      isCountryCodeCensoredByDefault && enabledState == SettingsValues.CensorshipCircumventionEnabled.DISABLED -> {
        CensorshipCircumventionState.AVAILABLE_MANUALLY_DISABLED
      }
      isCountryCodeCensoredByDefault -> {
        CensorshipCircumventionState.AVAILABLE_AUTOMATICALLY_ENABLED
      }
      !hasInternet && enabledState != SettingsValues.CensorshipCircumventionEnabled.ENABLED -> {
        CensorshipCircumventionState.UNAVAILABLE_NO_INTERNET
      }
      websocketConnected && enabledState != SettingsValues.CensorshipCircumventionEnabled.ENABLED -> {
        CensorshipCircumventionState.UNAVAILABLE_CONNECTED
      }
      else -> {
        CensorshipCircumventionState.AVAILABLE
      }
    }
  }

  private fun getCensorshipCircumventionEnabled(state: CensorshipCircumventionState): Boolean {
    return when (state) {
      CensorshipCircumventionState.UNAVAILABLE_CONNECTED,
      CensorshipCircumventionState.UNAVAILABLE_NO_INTERNET,
      CensorshipCircumventionState.AVAILABLE_MANUALLY_DISABLED -> {
        false
      }
      CensorshipCircumventionState.AVAILABLE_AUTOMATICALLY_ENABLED -> {
        true
      }
      else -> {
        ZonaRosaStore.settings.censorshipCircumventionEnabled == SettingsValues.CensorshipCircumventionEnabled.ENABLED
      }
    }
  }

  enum class Event {
    DISABLE_PUSH_FAILED
  }
}
