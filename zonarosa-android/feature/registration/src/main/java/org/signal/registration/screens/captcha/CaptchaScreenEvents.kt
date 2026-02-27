/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.captcha

sealed class CaptchaScreenEvents {
  data class CaptchaCompleted(val token: String) : CaptchaScreenEvents()
  data object Cancel : CaptchaScreenEvents()
}
