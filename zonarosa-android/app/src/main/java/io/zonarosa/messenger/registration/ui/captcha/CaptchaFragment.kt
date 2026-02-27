/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.captcha

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import io.zonarosa.core.ui.logging.LoggingFragment
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.ViewBinderDelegate
import io.zonarosa.messenger.databinding.FragmentRegistrationCaptchaBinding
import io.zonarosa.messenger.registration.fragments.RegistrationConstants

abstract class CaptchaFragment : LoggingFragment(R.layout.fragment_registration_captcha) {

  private val binding: FragmentRegistrationCaptchaBinding by ViewBinderDelegate(FragmentRegistrationCaptchaBinding::bind)

  @SuppressLint("SetJavaScriptEnabled")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.registrationCaptchaWebView.settings.javaScriptEnabled = true
    binding.registrationCaptchaWebView.clearCache(true)

    binding.registrationCaptchaWebView.webViewClient = object : WebViewClient() {
      @Deprecated("Deprecated in Java")
      override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (url.startsWith(RegistrationConstants.ZONAROSA_CAPTCHA_SCHEME)) {
          val token = url.substring(RegistrationConstants.ZONAROSA_CAPTCHA_SCHEME.length)
          handleCaptchaToken(token)
          findNavController().navigateUp()
          return true
        }
        return false
      }
    }
    binding.registrationCaptchaWebView.loadUrl(BuildConfig.ZONAROSA_CAPTCHA_URL)
  }

  abstract fun handleCaptchaToken(token: String)
}
