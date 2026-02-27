/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.backups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.DeletionState
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.Environment
import kotlin.time.Duration.Companion.milliseconds

class BackupsSettingsViewModel : ViewModel() {

  companion object {
    private val TAG = Log.tag(BackupsSettingsViewModel::class)
  }

  private val internalStateFlow: MutableStateFlow<BackupsSettingsState>

  val stateFlow: StateFlow<BackupsSettingsState> by lazy { internalStateFlow }

  init {
    val repo = BackupStateObserver(viewModelScope, useDatabaseFallbackOnNetworkError = true)
    internalStateFlow = MutableStateFlow(BackupsSettingsState(backupState = repo.backupState.value))

    viewModelScope.launch {
      repo.backupState.collect { enabledState ->
        Log.d(TAG, "Found enabled state $enabledState. Updating UI state.")
        internalStateFlow.update {
          it.copy(
            backupState = enabledState,
            lastBackupAt = ZonaRosaStore.backup.lastBackupTime.milliseconds,
            showBackupTierInternalOverride = Environment.IS_STAGING,
            backupTierInternalOverride = ZonaRosaStore.backup.backupTierInternalOverride
          )
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      ZonaRosaStore.backup.lastBackupTimeFlow
        .collect { lastBackupTime ->
          internalStateFlow.update {
            it.copy(lastBackupAt = lastBackupTime.milliseconds)
          }
        }
    }
  }

  fun onBackupTierInternalOverrideChanged(tier: MessageBackupTier?) {
    ZonaRosaStore.backup.backupTierInternalOverride = tier
    ZonaRosaStore.backup.deletionState = DeletionState.NONE
    viewModelScope.launch(ZonaRosaDispatchers.Default) {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }

    BackupStateObserver.notifyBackupStateChanged(scope = viewModelScope)
  }
}
