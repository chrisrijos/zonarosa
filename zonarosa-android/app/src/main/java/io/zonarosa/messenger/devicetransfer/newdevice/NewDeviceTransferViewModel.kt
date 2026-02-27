/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.devicetransfer.newdevice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.ReclaimUsernameAndLinkJob
import io.zonarosa.messenger.keyvalue.Completed
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.registration.data.RegistrationRepository
import io.zonarosa.messenger.registration.util.RegistrationUtil

class NewDeviceTransferViewModel : ViewModel() {
  fun onRestoreComplete(context: Context, onComplete: () -> Unit) {
    viewModelScope.launch {
      ZonaRosaStore.registration.localRegistrationMetadata?.let { metadata ->
        RegistrationRepository.registerAccountLocally(context, metadata)
        ZonaRosaStore.registration.localRegistrationMetadata = null
        RegistrationUtil.maybeMarkRegistrationComplete()

        AppDependencies.jobManager.add(ReclaimUsernameAndLinkJob())
      }

      ZonaRosaStore.registration.restoreDecisionState = RestoreDecisionState.Completed

      withContext(Dispatchers.Main) {
        onComplete()
      }
    }
  }
}
