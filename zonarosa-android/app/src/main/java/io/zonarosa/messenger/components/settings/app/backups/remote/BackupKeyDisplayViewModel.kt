/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.backups.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.StagedBackupKeyRotations
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.RestoreOptimizedMediaJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.service.api.NetworkResult

class BackupKeyDisplayViewModel : ViewModel(), BackupKeyCredentialManagerHandler {

  companion object {
    private val TAG = Log.tag(BackupKeyDisplayViewModel::class.java)
  }

  private val internalUiState = MutableStateFlow(BackupKeyDisplayUiState())
  val uiState: StateFlow<BackupKeyDisplayUiState> = internalUiState.asStateFlow()

  override fun updateBackupKeySaveState(newState: BackupKeySaveState?) {
    internalUiState.update { it.copy(keySaveState = newState) }
  }

  init {
    getKeyRotationLimit()
  }

  fun rotateBackupKey() {
    viewModelScope.launch {
      internalUiState.update { it.copy(rotationState = BackupKeyRotationState.GENERATING_KEY) }

      val stagedKeyRotations = withContext(ZonaRosaDispatchers.IO) {
        BackupRepository.stageBackupKeyRotations()
      }

      internalUiState.update {
        it.copy(
          accountEntropyPool = stagedKeyRotations.aep,
          stagedKeyRotations = stagedKeyRotations,
          rotationState = BackupKeyRotationState.USER_VERIFICATION
        )
      }
    }
  }

  fun commitBackupKey() {
    viewModelScope.launch {
      internalUiState.update { it.copy(rotationState = BackupKeyRotationState.COMMITTING_KEY) }

      val keyRotations = internalUiState.value.stagedKeyRotations ?: error("No key rotations to commit!")

      withContext(ZonaRosaDispatchers.IO) {
        BackupRepository.commitAEPKeyRotation(keyRotations)
      }

      internalUiState.update { it.copy(rotationState = BackupKeyRotationState.FINISHED) }
    }
  }

  fun getKeyRotationLimit() {
    viewModelScope.launch(ZonaRosaDispatchers.IO) {
      val result = BackupRepository.getKeyRotationLimit()
      if (result is NetworkResult.Success) {
        internalUiState.update {
          it.copy(
            canRotateKey = result.result.hasPermitsRemaining ?: true
          )
        }
      } else {
        Log.w(TAG, "Error while getting rotation limit: $result. Default to allowing key rotations.")
      }
    }
  }

  fun turnOffOptimizedStorageAndDownloadMedia() {
    ZonaRosaStore.backup.optimizeStorage = false
    // TODO - flag to notify when complete.
    AppDependencies.jobManager.add(RestoreOptimizedMediaJob())
  }
}

data class BackupKeyDisplayUiState(
  val accountEntropyPool: AccountEntropyPool = ZonaRosaStore.account.accountEntropyPool,
  val keySaveState: BackupKeySaveState? = null,
  val isOptimizedStorageEnabled: Boolean = ZonaRosaStore.backup.optimizeStorage,
  val rotationState: BackupKeyRotationState = BackupKeyRotationState.NOT_STARTED,
  val stagedKeyRotations: StagedBackupKeyRotations? = null,
  val canRotateKey: Boolean = true
)

enum class BackupKeyRotationState {
  NOT_STARTED,
  GENERATING_KEY,
  USER_VERIFICATION,
  COMMITTING_KEY,
  FINISHED
}
