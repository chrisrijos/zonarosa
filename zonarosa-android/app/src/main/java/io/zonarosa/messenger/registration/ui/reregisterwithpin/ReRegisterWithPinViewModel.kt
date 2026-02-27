/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.reregisterwithpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ReRegisterWithPinViewModel : ViewModel() {
  private val store = MutableStateFlow(ReRegisterWithPinState())

  val uiState = store.asLiveData()

  val isLocalVerification: Boolean
    get() = store.value.isLocalVerification

  fun markAsRemoteVerification() {
    store.update {
      it.copy(isLocalVerification = false)
    }
  }

  fun markIncorrectGuess() {
    store.update {
      it.copy(hasIncorrectGuess = true)
    }
  }

  fun toggleKeyboardType() {
    store.update {
      it.copy(pinKeyboardType = it.pinKeyboardType.other)
    }
  }
}
