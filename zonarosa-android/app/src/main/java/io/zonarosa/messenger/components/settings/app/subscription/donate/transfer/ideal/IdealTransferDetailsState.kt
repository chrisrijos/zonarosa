/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.donate.transfer.ideal

import io.zonarosa.donations.StripeApi
import io.zonarosa.messenger.components.settings.app.subscription.donate.transfer.BankDetailsValidator
import io.zonarosa.messenger.database.InAppPaymentTable

data class IdealTransferDetailsState(
  val inAppPayment: InAppPaymentTable.InAppPayment? = null,
  val name: String = "",
  val nameFocusState: FocusState = FocusState.NOT_FOCUSED,
  val email: String = "",
  val emailFocusState: FocusState = FocusState.NOT_FOCUSED
) {

  fun showNameError(): Boolean {
    return nameFocusState == FocusState.LOST_FOCUS && !BankDetailsValidator.validName(name)
  }

  fun showEmailError(): Boolean {
    return emailFocusState == FocusState.LOST_FOCUS && !BankDetailsValidator.validEmail(email)
  }

  fun asIDEALData(): StripeApi.IDEALData {
    return StripeApi.IDEALData(
      name = name.trim(),
      email = email.trim()
    )
  }

  fun canProceed(): Boolean {
    return BankDetailsValidator.validName(name) && (inAppPayment?.type?.recurring != true || BankDetailsValidator.validEmail(email))
  }

  enum class FocusState {
    NOT_FOCUSED,
    FOCUSED,
    LOST_FOCUS
  }
}
