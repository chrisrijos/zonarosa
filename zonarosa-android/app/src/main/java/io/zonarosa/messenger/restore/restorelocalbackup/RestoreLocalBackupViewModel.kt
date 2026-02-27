/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.restore.restorelocalbackup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.BackupEvent
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.ReclaimUsernameAndLinkJob
import io.zonarosa.messenger.keyvalue.Completed
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.registration.data.RegistrationRepository
import io.zonarosa.messenger.registration.util.RegistrationUtil
import io.zonarosa.messenger.restore.RestoreRepository

/**
 * ViewModel for [RestoreLocalBackupFragment]
 */
class RestoreLocalBackupViewModel(fileBackupUri: Uri) : ViewModel() {
  private val store = MutableStateFlow(RestoreLocalBackupState(fileBackupUri))
  val uiState = store.asLiveData()

  val backupReadError = store.map { it.backupFileStateError }.asLiveData()

  val importResult = store.map { it.backupImportResult }.asLiveData()

  fun prepareRestore(context: Context) {
    val backupFileUri = store.value.uri
    viewModelScope.launch {
      val result: RestoreRepository.BackupInfoResult = RestoreRepository.getLocalBackupFromUri(context, backupFileUri)

      if (result.failure && result.failureCause != null) {
        store.update {
          it.copy(
            backupFileStateError = result.failureCause.state
          )
        }
      } else if (result.backupInfo == null) {
        abort()
        return@launch
      }

      store.update {
        it.copy(
          backupInfo = result.backupInfo
        )
      }
    }
  }

  private fun abort() {
    store.update {
      it.copy(abort = true)
    }
  }

  fun confirmPassphraseAndBeginRestore(context: Context, passphrase: String) {
    store.update {
      it.copy(
        backupPassphrase = passphrase,
        restoreInProgress = true
      )
    }

    val backupFileUri = store.value.backupInfo?.uri
    val backupPassphrase = store.value.backupPassphrase
    if (backupFileUri == null) {
      Log.w(TAG, "Could not begin backup import because backup file URI was null!")
      abort()
      return
    }

    if (backupPassphrase.isEmpty()) {
      Log.w(TAG, "Could not begin backup import because backup passphrase was empty!")
      abort()
      return
    }

    viewModelScope.launch {
      val importResult: RestoreRepository.BackupImportResult = RestoreRepository.restoreBackupAsynchronously(context, backupFileUri, backupPassphrase)

      if (importResult == RestoreRepository.BackupImportResult.SUCCESS) {
        ZonaRosaStore.registration.localRegistrationMetadata?.let {
          RegistrationRepository.registerAccountLocally(context, it)
          ZonaRosaStore.registration.localRegistrationMetadata = null
          RegistrationUtil.maybeMarkRegistrationComplete()

          AppDependencies.jobManager.add(ReclaimUsernameAndLinkJob())
        }

        ZonaRosaStore.registration.restoreDecisionState = RestoreDecisionState.Completed
      }

      store.update {
        it.copy(
          backupImportResult = importResult,
          restoreInProgress = false,
          backupEstimatedTotalCount = -1L,
          backupProgressCount = -1L,
          backupVerifyingInProgress = false
        )
      }
    }
  }

  fun onBackupProgressUpdate(event: BackupEvent) {
    store.update {
      it.copy(
        backupProgressCount = event.count,
        backupEstimatedTotalCount = event.estimatedTotalCount,
        backupVerifyingInProgress = event.type == BackupEvent.Type.PROGRESS_VERIFYING
      )
    }
  }

  fun clearBackupFileStateError() {
    store.update { it.copy(backupFileStateError = null) }
  }

  fun backupImportErrorShown() {
    store.update {
      it.copy(
        backupImportResult = null
      )
    }
  }

  companion object {
    private val TAG = Log.tag(RestoreLocalBackupViewModel::class.java)
  }
}
