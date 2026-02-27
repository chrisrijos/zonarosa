/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.restore

import io.zonarosa.core.ui.compose.QrCodeData

sealed class QrState {
  data object Loading : QrState()
  data class Loaded(val qrCodeData: QrCodeData) : QrState()
  data object Scanned : QrState()
  data object Failed : QrState()
}

data class RestoreViaQrState(
  val qrState: QrState = QrState.Loading,
  val isRegistering: Boolean = false,
  val showRegistrationError: Boolean = false,
  val errorMessage: String? = null
)
