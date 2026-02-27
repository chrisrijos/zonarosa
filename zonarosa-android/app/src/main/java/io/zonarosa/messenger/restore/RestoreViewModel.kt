/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.restore

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.Skipped
import io.zonarosa.messenger.keyvalue.includeDeviceToDeviceTransfer
import io.zonarosa.messenger.keyvalue.skippedRestoreChoice
import io.zonarosa.messenger.registration.data.QuickRegistrationRepository
import io.zonarosa.messenger.registration.ui.restore.RestoreMethod
import io.zonarosa.messenger.registration.ui.restore.StorageServiceRestore
import io.zonarosa.service.api.provisioning.RestoreMethod as ApiRestoreMethod

/**
 * Shared view model for the restore flow.
 */
class RestoreViewModel : ViewModel() {
  private val store = MutableStateFlow(RestoreState())
  val uiState = store.asLiveData()

  var showStorageAccountRestoreProgress by mutableStateOf(false)
    private set

  fun setNextIntent(nextIntent: Intent) {
    store.update {
      it.copy(nextIntent = nextIntent)
    }
  }

  fun setBackupFileUri(backupFileUri: Uri) {
    store.update {
      it.copy(backupFile = backupFileUri)
    }
  }

  fun getBackupFileUri(): Uri? = store.value.backupFile

  fun getNextIntent(): Intent? = store.value.nextIntent

  fun hasNoRestoreMethods(): Boolean {
    return getAvailableRestoreMethods().isEmpty()
  }

  fun getAvailableRestoreMethods(): List<RestoreMethod> {
    if (ZonaRosaStore.registration.isOtherDeviceAndroid || ZonaRosaStore.registration.restoreDecisionState.skippedRestoreChoice) {
      val methods = mutableListOf(RestoreMethod.FROM_LOCAL_BACKUP_V1)

      if (ZonaRosaStore.registration.isOtherDeviceAndroid && ZonaRosaStore.registration.restoreDecisionState.includeDeviceToDeviceTransfer) {
        methods.add(0, RestoreMethod.FROM_OLD_DEVICE)
      }

      when (ZonaRosaStore.backup.backupTier) {
        MessageBackupTier.FREE -> methods.add(1, RestoreMethod.FROM_ZONAROSA_BACKUPS)
        MessageBackupTier.PAID -> methods.add(0, RestoreMethod.FROM_ZONAROSA_BACKUPS)
        null -> if (!ZonaRosaStore.backup.restoringViaQr) {
          methods.add(1, RestoreMethod.FROM_ZONAROSA_BACKUPS)
        }
      }

      return methods
    }

    if (ZonaRosaStore.backup.restoringViaQr && ZonaRosaStore.backup.backupTier != null) {
      return listOf(RestoreMethod.FROM_ZONAROSA_BACKUPS)
    }

    return emptyList()
  }

  fun hasRestoredAccountEntropyPool(): Boolean {
    return ZonaRosaStore.account.restoredAccountEntropyPool
  }

  fun hasRestoredBackupDataFromQr(): Boolean {
    return ZonaRosaStore.backup.restoringViaQr && ZonaRosaStore.backup.backupTier != null
  }

  fun skipRestore() {
    ZonaRosaStore.registration.restoreDecisionState = RestoreDecisionState.Skipped

    viewModelScope.launch {
      QuickRegistrationRepository.setRestoreMethodForOldDevice(ApiRestoreMethod.DECLINE)
    }
  }

  suspend fun performStorageServiceAccountRestoreIfNeeded() {
    if (hasRestoredAccountEntropyPool() || ZonaRosaStore.svr.masterKeyForInitialDataRestore != null) {
      showStorageAccountRestoreProgress = true
      StorageServiceRestore.restore()
    }
  }
}
