package io.zonarosa.messenger.components.settings.app.account

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.ZonaRosaPreferences

class AccountSettingsViewModel : ViewModel() {
  private val store: MutableStateFlow<AccountSettingsState> = MutableStateFlow(getCurrentState())

  val state: StateFlow<AccountSettingsState> = store

  fun refreshState() {
    store.update { getCurrentState() }
  }

  fun togglePinKeyboardType() {
    store.update {
      it.copy(pinKeyboardType = it.pinKeyboardType.other)
    }
  }

  private fun getCurrentState(): AccountSettingsState {
    return AccountSettingsState(
      hasPin = ZonaRosaStore.svr.hasPin() && !ZonaRosaStore.svr.hasOptedOut(),
      pinKeyboardType = ZonaRosaStore.pin.keyboardType,
      hasRestoredAep = ZonaRosaStore.account.restoredAccountEntropyPool,
      pinRemindersEnabled = ZonaRosaStore.pin.arePinRemindersEnabled() && ZonaRosaStore.svr.hasPin(),
      registrationLockEnabled = ZonaRosaStore.svr.isRegistrationLockEnabled,
      userUnregistered = ZonaRosaPreferences.isUnauthorizedReceived(AppDependencies.application),
      clientDeprecated = ZonaRosaStore.misc.isClientDeprecated,
      canTransferWhileUnregistered = true
    )
  }
}
