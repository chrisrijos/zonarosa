/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.updates

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import kotlin.time.Duration.Companion.milliseconds

class AppUpdatesSettingsViewModel : ViewModel() {
  private val internalState = MutableStateFlow(getState())

  val state: StateFlow<AppUpdatesSettingsState> = internalState

  fun refresh() {
    internalState.update { getState() }
  }

  private fun getState(): AppUpdatesSettingsState {
    return AppUpdatesSettingsState(
      lastCheckedTime = ZonaRosaStore.apkUpdate.lastSuccessfulCheck.milliseconds,
      autoUpdateEnabled = ZonaRosaStore.apkUpdate.autoUpdate
    )
  }
}
