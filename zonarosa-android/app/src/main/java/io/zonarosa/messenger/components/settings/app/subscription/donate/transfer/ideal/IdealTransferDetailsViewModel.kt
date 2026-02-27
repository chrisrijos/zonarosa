/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.donate.transfer.ideal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase

class IdealTransferDetailsViewModel(inAppPaymentId: InAppPaymentTable.InAppPaymentId) : ViewModel() {

  private val internalState = MutableStateFlow(IdealTransferDetailsState())
  var state: StateFlow<IdealTransferDetailsState> = internalState

  init {
    viewModelScope.launch {
      val inAppPayment = withContext(Dispatchers.IO) {
        ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)!!
      }

      internalState.update {
        it.copy(inAppPayment = inAppPayment)
      }
    }
  }

  fun onNameChanged(name: String) {
    internalState.update {
      it.copy(name = name)
    }
  }

  fun onEmailChanged(email: String) {
    internalState.update {
      it.copy(email = email)
    }
  }

  fun onFocusChanged(field: Field, isFocused: Boolean) {
    internalState.update { state ->
      when (field) {
        Field.NAME -> {
          if (isFocused && state.nameFocusState == IdealTransferDetailsState.FocusState.NOT_FOCUSED) {
            state.copy(nameFocusState = IdealTransferDetailsState.FocusState.FOCUSED)
          } else if (!isFocused && state.nameFocusState == IdealTransferDetailsState.FocusState.FOCUSED) {
            state.copy(nameFocusState = IdealTransferDetailsState.FocusState.LOST_FOCUS)
          } else {
            state
          }
        }

        Field.EMAIL -> {
          if (isFocused && state.emailFocusState == IdealTransferDetailsState.FocusState.NOT_FOCUSED) {
            state.copy(emailFocusState = IdealTransferDetailsState.FocusState.FOCUSED)
          } else if (!isFocused && state.emailFocusState == IdealTransferDetailsState.FocusState.FOCUSED) {
            state.copy(emailFocusState = IdealTransferDetailsState.FocusState.LOST_FOCUS)
          } else {
            state
          }
        }
      }
    }
  }

  enum class Field {
    NAME,
    EMAIL
  }
}
