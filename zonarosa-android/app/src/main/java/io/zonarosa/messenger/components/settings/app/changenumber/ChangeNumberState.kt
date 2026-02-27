/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.changenumber

import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.lock.v2.PinKeyboardType
import io.zonarosa.messenger.registration.data.network.Challenge
import io.zonarosa.messenger.registration.data.network.VerificationCodeRequestResult
import io.zonarosa.messenger.registration.ui.countrycode.Country
import io.zonarosa.messenger.registration.viewmodel.NumberViewState
import io.zonarosa.service.api.svr.Svr3Credentials
import io.zonarosa.service.internal.push.AuthCredentials

/**
 * State holder for [ChangeNumberViewModel]
 */
data class ChangeNumberState(
  val number: NumberViewState = NumberViewState.INITIAL,
  val enteredCode: String? = null,
  val enteredPin: String = "",
  val pinKeyboardType: PinKeyboardType = ZonaRosaStore.pin.keyboardType,
  val oldPhoneNumber: NumberViewState = NumberViewState.INITIAL,
  val sessionId: String? = null,
  val changeNumberOutcome: ChangeNumberOutcome? = null,
  val lockedTimeRemaining: Long = 0L,
  val svr2Credentials: AuthCredentials? = null,
  val svr3Credentials: Svr3Credentials? = null,
  val svrTriesRemaining: Int = 10,
  val incorrectCodeAttempts: Int = 0,
  val nextSmsTimestamp: Long = 0L,
  val nextCallTimestamp: Long = 0L,
  val inProgress: Boolean = false,
  val captchaToken: String? = null,
  val challengesRequested: List<Challenge> = emptyList(),
  val challengesPresented: Set<Challenge> = emptySet(),
  val allowedToRequestCode: Boolean = false,
  val oldCountry: Country? = null,
  val newCountry: Country? = null,
  val challengeInProgress: Boolean = false
)

sealed interface ChangeNumberOutcome {
  data object RecoveryPasswordWorked : ChangeNumberOutcome
  data object VerificationCodeWorked : ChangeNumberOutcome
  class ChangeNumberRequestOutcome(val result: VerificationCodeRequestResult) : ChangeNumberOutcome
}

sealed interface ChangeLocalNumberOutcome {
  data object NotPerformed : ChangeLocalNumberOutcome
  data object Success : ChangeLocalNumberOutcome
  data object Failure : ChangeLocalNumberOutcome
}
