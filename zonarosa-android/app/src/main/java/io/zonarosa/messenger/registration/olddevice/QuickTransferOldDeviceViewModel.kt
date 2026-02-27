/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.olddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.registration.data.QuickRegistrationRepository
import io.zonarosa.messenger.registration.olddevice.QuickTransferOldDeviceState
import io.zonarosa.messenger.registration.olddevice.preparedevice.PrepareDeviceScreenEvents
import io.zonarosa.messenger.registration.olddevice.transferaccount.TransferScreenEvents
import io.zonarosa.service.api.provisioning.RestoreMethod
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class QuickTransferOldDeviceViewModel(reRegisterUri: String) : ViewModel() {

  companion object {
    private val TAG = Log.tag(QuickTransferOldDeviceViewModel::class)
  }

  private val store: MutableStateFlow<QuickTransferOldDeviceState> = MutableStateFlow(
    QuickTransferOldDeviceState(
      reRegisterUri = reRegisterUri,
      lastBackupTimestamp = ZonaRosaStore.backup.lastBackupTime
    )
  )

  val state: StateFlow<QuickTransferOldDeviceState> = store

  private val _backStack: MutableStateFlow<List<TransferAccountRoute>> = MutableStateFlow(listOf(TransferAccountRoute.Transfer))
  val backStack: StateFlow<List<TransferAccountRoute>> = _backStack

  fun goBack() {
    _backStack.update { it.dropLast(1) }
  }

  fun onEvent(event: PrepareDeviceScreenEvents) {
    when (event) {
      PrepareDeviceScreenEvents.BackUpNow -> {
        store.update { it.copy(navigateToBackupCreation = true) }
      }
      PrepareDeviceScreenEvents.NavigateBack -> {
        _backStack.update { it.dropLast(1) }
      }
      PrepareDeviceScreenEvents.SkipAndContinue -> {
        _backStack.update { listOf(TransferAccountRoute.Transfer) }
        transferAccount()
      }
    }
  }

  fun onEvent(event: TransferScreenEvents) {
    when (event) {
      TransferScreenEvents.ContinueOnOtherDeviceDismiss -> {
        _backStack.update { listOf(TransferAccountRoute.Done) }
      }
      TransferScreenEvents.ErrorDialogDismissed -> {
        store.update { it.copy(reRegisterResult = null) }
      }
      TransferScreenEvents.NavigateBack -> {
        _backStack.update { listOf(TransferAccountRoute.Done) }
      }
      TransferScreenEvents.TransferClicked -> {
        store.update { it.copy(performAuthentication = true) }
      }
    }
  }

  fun onTransferAccountAttempted() {
    val timeSinceLastBackup = (System.currentTimeMillis() - store.value.lastBackupTimestamp).milliseconds
    if (timeSinceLastBackup > 30.minutes) {
      Log.i(TAG, "It's been $timeSinceLastBackup since the last backup. Prompting user to back up now.")
      _backStack.update { it + TransferAccountRoute.PrepareDevice }
    } else {
      Log.i(TAG, "It's been $timeSinceLastBackup since the last backup. We can continue without prompting.")
      transferAccount()
    }
  }

  fun clearAttemptAuthentication() {
    store.update { it.copy(performAuthentication = false) }
  }

  fun clearNavigateToBackupCreation() {
    store.update { it.copy(navigateToBackupCreation = false) }
  }

  private fun transferAccount() {
    viewModelScope.launch(Dispatchers.IO) {
      val restoreMethodToken = UUID.randomUUID().toString()
      store.update { it.copy(inProgress = true) }
      val result = QuickRegistrationRepository.transferAccount(store.value.reRegisterUri, restoreMethodToken)
      store.update { it.copy(reRegisterResult = result, inProgress = false) }

      if (result == QuickRegistrationRepository.TransferAccountResult.SUCCESS) {
        val restoreMethod = QuickRegistrationRepository.waitForRestoreMethodSelectionOnNewDevice(restoreMethodToken)

        if (restoreMethod != RestoreMethod.DECLINE) {
          ZonaRosaStore.Companion.registration.restoringOnNewDevice = true
        }

        store.update { it.copy(restoreMethodSelected = restoreMethod) }
      }
    }
  }
}
