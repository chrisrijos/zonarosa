/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.usernamelinks

sealed class QrCodeState {
  /** QR code data exists and is available. */
  data class Present(val data: QrCodeData) : QrCodeState()

  /** QR code data does not exist. */
  object NotSet : QrCodeState()

  /** QR code data is in an indeterminate loading state. */
  object Loading : QrCodeState()
}
