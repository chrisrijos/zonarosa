/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.restore.enterbackupkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.RestoreTimestampResult
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.registration.ui.restore.AccountEntropyPoolVerification
import io.zonarosa.messenger.registration.ui.restore.AccountEntropyPoolVerification.AEPValidationError

class PostRegistrationEnterBackupKeyViewModel : ViewModel() {
  companion object {
    private val TAG = Log.tag(PostRegistrationEnterBackupKeyViewModel::class)
  }

  private val store = MutableStateFlow(
    PostRegistrationEnterBackupKeyState()
  )

  var backupKey by mutableStateOf("")
    private set

  val state: StateFlow<PostRegistrationEnterBackupKeyState> = store

  fun updateBackupKey(key: String) {
    val newKey = AccountEntropyPool.removeIllegalCharacters(key).take(AccountEntropyPool.LENGTH + 16).lowercase()
    val changed = newKey != backupKey
    backupKey = newKey
    store.update {
      val (isValid, updatedError) = AccountEntropyPoolVerification.verifyAEP(
        backupKey = backupKey,
        changed = changed,
        previousAEPValidationError = it.aepValidationError
      )
      it.copy(backupKeyValid = isValid, aepValidationError = updatedError)
    }
  }

  fun restoreBackupTimestamp() {
    store.update { it.copy(inProgress = true) }
    viewModelScope.launch(Dispatchers.IO) {
      val aep = AccountEntropyPool.parseOrNull(backupKey)

      val restoreTimestampResult = if (aep != null) {
        BackupRepository.verifyBackupKeyAssociatedWithAccount(ZonaRosaStore.account.requireAci(), aep)
      } else {
        Log.w(TAG, "Parsed AEP is null, failing")
        store.update { it.copy(aepValidationError = AEPValidationError.Invalid, errorDialog = ErrorDialog.AEP_INVALID) }
        return@launch
      }

      when (restoreTimestampResult) {
        is RestoreTimestampResult.Success -> {
          Log.i(TAG, "Backup timestamp found with entered AEP, migrating to new AEP and moving on to restore")
          ZonaRosaStore.account.restoreAccountEntropyPool(aep)
          store.update { it.copy(restoreBackupTierSuccessful = true) }
        }

        RestoreTimestampResult.NotFound -> store.update { it.copy(errorDialog = ErrorDialog.BACKUP_NOT_FOUND) }
        RestoreTimestampResult.BackupsNotEnabled -> store.update { it.copy(errorDialog = ErrorDialog.BACKUPS_NOT_ENABLED) }
        is RestoreTimestampResult.RateLimited -> store.update { it.copy(errorDialog = ErrorDialog.RATE_LIMITED) }
        else -> store.update { it.copy(errorDialog = ErrorDialog.UNKNOWN_ERROR) }
      }
    }
  }

  fun hideErrorDialog() {
    store.update {
      it.copy(errorDialog = null, inProgress = false)
    }
  }

  data class PostRegistrationEnterBackupKeyState(
    val backupKeyValid: Boolean = false,
    val inProgress: Boolean = false,
    val restoreBackupTierSuccessful: Boolean = false,
    val aepValidationError: AEPValidationError? = null,
    val errorDialog: ErrorDialog? = null
  )

  enum class ErrorDialog {
    AEP_INVALID,
    BACKUPS_NOT_ENABLED,
    BACKUP_NOT_FOUND,
    RATE_LIMITED,
    UNKNOWN_ERROR
  }
}
