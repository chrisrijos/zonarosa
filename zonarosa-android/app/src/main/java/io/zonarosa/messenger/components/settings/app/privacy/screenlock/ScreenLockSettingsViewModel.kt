package io.zonarosa.messenger.components.settings.app.privacy.screenlock

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Maintains the state of the [ScreenLockSettingsFragment]
 */
class ScreenLockSettingsViewModel : ViewModel() {

  private val _state = MutableStateFlow(getState())
  val state = _state.asStateFlow()

  fun toggleScreenLock() {
    val enabled = !_state.value.screenLock
    ZonaRosaStore.settings.screenLockEnabled = enabled
    _state.update {
      it.copy(
        screenLock = enabled
      )
    }
  }

  fun setScreenLockTimeout(seconds: Long) {
    ZonaRosaStore.settings.screenLockTimeout = seconds
    _state.update {
      it.copy(
        screenLockActivityTimeout = seconds
      )
    }
  }

  private fun getState(): ScreenLockSettingsState {
    return ScreenLockSettingsState(
      screenLock = ZonaRosaStore.settings.screenLockEnabled,
      screenLockActivityTimeout = ZonaRosaStore.settings.screenLockTimeout
    )
  }
}
