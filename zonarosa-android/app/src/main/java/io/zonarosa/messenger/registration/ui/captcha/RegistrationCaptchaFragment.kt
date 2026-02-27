/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.captcha

import androidx.fragment.app.activityViewModels
import io.zonarosa.messenger.registration.ui.RegistrationViewModel

/**
 * Screen that displays a captcha as part of the registration flow.
 * This subclass plugs in [RegistrationViewModel] to the shared super class.
 *
 * @see CaptchaFragment
 */
class RegistrationCaptchaFragment : CaptchaFragment() {
  private val sharedViewModel by activityViewModels<RegistrationViewModel>()

  override fun handleCaptchaToken(token: String) {
    sharedViewModel.setCaptchaResponse(token)
  }
}
