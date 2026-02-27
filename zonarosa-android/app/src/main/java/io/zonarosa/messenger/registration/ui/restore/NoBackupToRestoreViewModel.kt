/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.registration.proto.RegistrationProvisionMessage
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.Skipped
import io.zonarosa.messenger.registration.data.QuickRegistrationRepository
import io.zonarosa.messenger.registration.data.network.RegisterAccountResult
import io.zonarosa.service.api.provisioning.RestoreMethod

class NoBackupToRestoreViewModel(decode: RegistrationProvisionMessage) : ViewModel() {
  companion object {
    private val TAG = Log.tag(NoBackupToRestoreViewModel::class)
  }

  private val store: MutableStateFlow<NoBackupToRestoreState> = MutableStateFlow(NoBackupToRestoreState(provisioningMessage = decode))

  val state: StateFlow<NoBackupToRestoreState> = store

  fun skipRestoreAndRegister() {
    ZonaRosaStore.registration.restoreDecisionState = RestoreDecisionState.Skipped
    store.update { it.copy(isRegistering = true) }

    viewModelScope.launch(Dispatchers.IO) {
      QuickRegistrationRepository.setRestoreMethodForOldDevice(RestoreMethod.DECLINE)
    }
  }

  fun handleRegistrationFailure(registerAccountResult: RegisterAccountResult) {
    store.update {
      if (it.isRegistering) {
        Log.w(TAG, "Unable to register [${registerAccountResult::class.simpleName}]", registerAccountResult.getCause(), true)
        it.copy(
          isRegistering = false,
          showRegistrationError = true,
          registerAccountResult = registerAccountResult
        )
      } else {
        it
      }
    }
  }

  fun clearRegistrationError() {
    store.update {
      it.copy(
        showRegistrationError = false,
        registerAccountResult = null
      )
    }
  }

  data class NoBackupToRestoreState(
    val isRegistering: Boolean = false,
    val provisioningMessage: RegistrationProvisionMessage,
    val showRegistrationError: Boolean = false,
    val registerAccountResult: RegisterAccountResult? = null
  )
}
