/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.pinentry

import kotlin.time.Duration

data class PinEntryState(
  val showNeedHelp: Boolean = false,
  val isAlphanumericKeyboard: Boolean = false,
  val loading: Boolean = false,
  val triesRemaining: Int? = null,
  val mode: Mode = Mode.SvrRestore,
  val oneTimeEvent: OneTimeEvent? = null,
  val e164: String? = null
) {
  enum class Mode {
    RegistrationLock,
    SmsBypass,
    SvrRestore
  }

  sealed interface OneTimeEvent {
    data object NetworkError : OneTimeEvent
    data class RateLimited(val retryAfter: Duration) : OneTimeEvent
    data object SvrDataMissing : OneTimeEvent
    data object UnknownError : OneTimeEvent
  }
}
