/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.ui

import android.view.Window
import android.view.WindowManager

/**
 * Initializes screenshot security on the window based on user preferences.
 */
fun Window.initializeScreenshotSecurity() {
  if (CoreUiDependencies.isScreenSecurityEnabled) {
    addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  } else {
    clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
  }
}
