/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.restore

sealed class RestoreViaQrScreenEvents {
  data object RetryQrCode : RestoreViaQrScreenEvents()
  data object Cancel : RestoreViaQrScreenEvents()
  data object UseProxy : RestoreViaQrScreenEvents()
  data object DismissError : RestoreViaQrScreenEvents()
}
