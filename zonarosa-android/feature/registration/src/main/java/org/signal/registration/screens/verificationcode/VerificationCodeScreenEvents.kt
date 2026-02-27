/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.verificationcode

sealed class VerificationCodeScreenEvents {
  data class CodeEntered(val code: String) : VerificationCodeScreenEvents()
  data object WrongNumber : VerificationCodeScreenEvents()
  data object ResendSms : VerificationCodeScreenEvents()
  data object CallMe : VerificationCodeScreenEvents()
  data object HavingTrouble : VerificationCodeScreenEvents()
  data object ConsumeInnerOneTimeEvent : VerificationCodeScreenEvents()

  /**
   * Event to update countdown timers. Should be triggered periodically (e.g., every second).
   */
  data object CountdownTick : VerificationCodeScreenEvents()
}
