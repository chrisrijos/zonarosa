/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.olddevice.transferaccount

sealed interface TransferScreenEvents {
  data object TransferClicked : TransferScreenEvents
  data object ContinueOnOtherDeviceDismiss : TransferScreenEvents
  data object ErrorDialogDismissed : TransferScreenEvents
  data object NavigateBack : TransferScreenEvents
}
