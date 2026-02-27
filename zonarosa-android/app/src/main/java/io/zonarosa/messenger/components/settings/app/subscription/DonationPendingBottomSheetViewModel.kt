/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase

class DonationPendingBottomSheetViewModel(
  inAppPaymentId: InAppPaymentTable.InAppPaymentId
) : ViewModel() {

  private val internalInAppPayment = MutableStateFlow<InAppPaymentTable.InAppPayment?>(null)
  val inAppPayment: StateFlow<InAppPaymentTable.InAppPayment?> = internalInAppPayment

  init {
    viewModelScope.launch {
      val inAppPayment = withContext(ZonaRosaDispatchers.IO) {
        ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)!!
      }

      internalInAppPayment.update { inAppPayment }
    }
  }
}
